package de.timonso.twitchchatmonitor

import de.timonso.twitchchatmonitor.config.Config
import de.timonso.twitchchatmonitor.database.DatabaseFactory
import de.timonso.twitchchatmonitor.listener.ChatListener
import de.timonso.twitchchatmonitor.listener.ModerationListener
import de.timonso.twitchchatmonitor.listener.StreamEventListener
import de.timonso.twitchchatmonitor.logs.CrashHandler
import de.timonso.twitchchatmonitor.tracker.EventTracker
import de.timonso.twitchchatmonitor.tracker.MessageTracker
import de.timonso.twitchchatmonitor.tracker.ModerationTracker
import de.timonso.twitchchatmonitor.tracker.UserTracker
import de.timonso.twitchchatmonitor.twitch.ChannelSeeder
import de.timonso.twitchchatmonitor.twitch.ChannelSync
import de.timonso.twitchchatmonitor.twitch.TwitchClientFactory
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("TwitchChatMonitor")
    CrashHandler.install()

    log.info("TwitchChatMonitor startet...")
    val config = Config.load()
    val settings = config.settings
    DatabaseFactory.init(config.db)

    val messageTracker = MessageTracker(settings.messageBatchSize, settings.messageFlushIntervalMs)
    val moderationTracker = ModerationTracker(settings.messageBatchSize, settings.messageFlushIntervalMs)
    val eventTracker = EventTracker(settings.messageBatchSize, settings.messageFlushIntervalMs)
    val userTracker = UserTracker(settings.userFlushIntervalSeconds)
    messageTracker.start()
    moderationTracker.start()
    eventTracker.start()
    userTracker.start()

    ChannelSeeder.seed(settings.seedChannels)
    val client = TwitchClientFactory.create(config.twitchAccessToken)

    ChatListener(messageTracker, userTracker).register(client.eventManager)
    ModerationListener(moderationTracker).register(client.eventManager)
    StreamEventListener(eventTracker).register(client.eventManager)

    val sync = ChannelSync(client.chat, settings.channelSyncIntervalSeconds)
    sync.start()

    Heartbeat.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Fahre herunter...")
        Heartbeat.stop()
        sync.stop()
        client.close()
        messageTracker.stop()
        moderationTracker.stop()
        eventTracker.stop()
        userTracker.stop()
        log.info("Shutdown abgeschlossen")
    })

    log.info("TwitchChatMonitor läuft, Channels werden alle {}s mit der Datenbank abgeglichen", settings.channelSyncIntervalSeconds)
}
