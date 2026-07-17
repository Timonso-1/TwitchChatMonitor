package de.timonso.twitchchatmonitor.tracker

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class BatchTracker<T : Any>(
    threadName: String,
    private val batchSize: Int,
    private val flushIntervalMs: Long,
) {
    protected val log: Logger = LoggerFactory.getLogger(javaClass)
    private val queue = LinkedBlockingQueue<T>(50_000)

    @Volatile
    private var running = true
    private val thread = Thread(::run, threadName)

    fun start() {
        thread.start()
    }

    fun enqueue(item: T) {
        if (!queue.offer(item)) {
            log.warn("Queue voll, Eintrag verworfen")
        }
    }

    private fun run() {
        val batch = ArrayList<T>(batchSize)
        while (running || queue.isNotEmpty()) {
            val first = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS) ?: continue
            batch.add(first)
            queue.drainTo(batch, batchSize - 1)
            try {
                persist(batch)
            } catch (e: Exception) {
                log.error("Konnte {} Einträge nicht speichern", batch.size, e)
            }
            batch.clear()
        }
    }

    protected abstract fun persist(batch: List<T>)

    fun stop() {
        running = false
        thread.join(10_000)
    }
}
