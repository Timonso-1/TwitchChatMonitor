package de.timonso.twitchchatmonitor.twitch

import com.github.twitch4j.chat.TwitchChat
import de.timonso.twitchchatmonitor.database.ChannelRegistry
import de.timonso.twitchchatmonitor.database.ChannelRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChannelSync(
    private val chat: TwitchChat,
    private val intervalSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "channel-sync").apply { isDaemon = true }
    }

    fun start() {
        scheduler.scheduleWithFixedDelay(::sync, 0, intervalSeconds, TimeUnit.SECONDS)
    }

    private fun sync() {
        try {
            val channels = ChannelRepository.loadAll()
            ChannelRegistry.update(channels)

            val desired = channels.filter { it.active }.map { it.name.lowercase() }.toSet()
            val joined = chat.channels.map { it.lowercase() }.toSet()

            (desired - joined).forEach {
                chat.joinChannel(it)
                log.info("Channel beigetreten: {}", it)
            }
            (joined - desired).forEach {
                chat.leaveChannel(it)
                log.info("Channel verlassen: {}", it)
            }
        } catch (e: Exception) {
            log.error("Channel-Sync fehlgeschlagen", e)
        }
    }

    fun stop() {
        scheduler.shutdownNow()
    }
}
