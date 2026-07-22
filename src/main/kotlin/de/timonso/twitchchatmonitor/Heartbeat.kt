package de.timonso.twitchchatmonitor

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.concurrent.thread

object Heartbeat {
    private val log = LoggerFactory.getLogger("Heartbeat")
    private const val PUSH_URL = "Push: https://status.timonso.de/api/push/YhEp1opik4?status=up&msg=OK&ping="
    private const val INTERVAL_MS = 30_000L

    @Volatile private var running = false

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun start() {
        running = true
        thread(name = "kuma-heartbeat", isDaemon = true) {
            while (running) {
                try {
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create(PUSH_URL))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build()
                    client.send(request, HttpResponse.BodyHandlers.discarding())
                } catch (e: Exception) {
                    log.warn("Heartbeat-Ping fehlgeschlagen: {}", e.message)
                }
                Thread.sleep(INTERVAL_MS)
            }
        }
    }

    fun stop() {
        running = false
    }
}