package de.timonso.twitchchatmonitor.listener

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.chat.events.channel.ClearChatEvent
import com.github.twitch4j.chat.events.channel.DeleteMessageEvent
import com.github.twitch4j.chat.events.channel.UserBanEvent
import com.github.twitch4j.chat.events.channel.UserTimeoutEvent
import com.github.twitch4j.common.events.domain.EventChannel
import de.timonso.twitchchatmonitor.database.ChannelRegistry
import de.timonso.twitchchatmonitor.tracker.ModerationRecord
import de.timonso.twitchchatmonitor.tracker.ModerationTracker
import org.slf4j.LoggerFactory
import java.time.Instant

class ModerationListener(private val tracker: ModerationTracker) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun register(eventManager: EventManager) {
        eventManager.onEvent(UserTimeoutEvent::class.java) { event ->
            record(
                channel = event.channel,
                action = "timeout",
                targetUserId = event.user?.id,
                targetUserName = event.user?.name,
                durationSeconds = event.duration,
                reason = event.reason,
            )
        }
        eventManager.onEvent(UserBanEvent::class.java) { event ->
            record(
                channel = event.channel,
                action = "ban",
                targetUserId = event.user?.id,
                targetUserName = event.user?.name,
            )
        }
        eventManager.onEvent(DeleteMessageEvent::class.java) { event ->
            record(
                channel = event.channel,
                action = "delete",
                targetUserName = event.userName,
                deletedMessageId = event.msgId,
                deletedMessageContent = event.message,
            )
        }
        eventManager.onEvent(ClearChatEvent::class.java) { event ->
            record(channel = event.channel, action = "clear_chat")
        }
    }

    private fun record(
        channel: EventChannel,
        action: String,
        targetUserId: String? = null,
        targetUserName: String? = null,
        durationSeconds: Int? = null,
        reason: String? = null,
        deletedMessageId: String? = null,
        deletedMessageContent: String? = null,
    ) {
        val info = ChannelRegistry.byBroadcasterId(channel.id)
        if (info == null) {
            log.warn("Moderationsaktion aus unbekanntem Channel {} ({}) wird verworfen", channel.name, channel.id)
            return
        }
        tracker.enqueue(
            ModerationRecord(
                channelUuid = info.uuid,
                channelId = channel.id,
                channelName = channel.name,
                action = action,
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                durationSeconds = durationSeconds,
                reason = reason,
                deletedMessageId = deletedMessageId,
                deletedMessageContent = deletedMessageContent,
                occurredAt = Instant.now(),
            )
        )
    }
}
