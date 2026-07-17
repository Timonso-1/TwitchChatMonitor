package de.timonso.twitchchatmonitor.twitch

import de.timonso.twitchchatmonitor.config.SeedChannel
import de.timonso.twitchchatmonitor.database.ChannelRepository
import org.slf4j.LoggerFactory

object ChannelSeeder {
    private val log = LoggerFactory.getLogger(javaClass)

    fun seed(channels: List<SeedChannel>) {
        channels.forEach { channel ->
            val inserted = ChannelRepository.insertIfMissing(
                broadcasterId = channel.twitchId,
                name = channel.name.lowercase(),
                createdByName = "seed",
            )
            if (inserted) {
                log.info("Channel '{}' (ID {}) aus config.json angelegt", channel.name, channel.twitchId)
            }
        }
    }
}
