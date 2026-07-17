package de.timonso.twitchchatmonitor.listener

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.chat.events.AbstractChannelMessageEvent
import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent
import com.github.twitch4j.chat.events.channel.CheerEvent
import com.github.twitch4j.chat.events.channel.IRCMessageEvent
import com.github.twitch4j.common.events.domain.EventChannel
import com.github.twitch4j.common.events.domain.EventUser
import de.timonso.twitchchatmonitor.database.ChannelRegistry
import de.timonso.twitchchatmonitor.tracker.ChatMessageRecord
import de.timonso.twitchchatmonitor.tracker.MessageTracker
import de.timonso.twitchchatmonitor.tracker.UserTracker
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class ChatListener(
    private val messageTracker: MessageTracker,
    private val userTracker: UserTracker,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun register(eventManager: EventManager) {
        eventManager.onEvent(AbstractChannelMessageEvent::class.java) { event ->
            handle(event.channel, event.user, event.message, event.messageEvent, event is ChannelMessageActionEvent)
        }
        eventManager.onEvent(CheerEvent::class.java) { event ->
            handle(event.channel, event.user, event.message, event.messageEvent, isAction = false)
        }
    }

    private fun handle(
        channel: EventChannel,
        user: EventUser,
        message: String,
        irc: IRCMessageEvent,
        isAction: Boolean,
    ) {
        val info = ChannelRegistry.byBroadcasterId(channel.id)
        if (info == null) {
            log.warn("Nachricht aus unbekanntem Channel {} ({}) wird verworfen", channel.name, channel.id)
            return
        }

        fun tag(key: String): String? = irc.getTagValue(key).orElse(null)

        val badges = tag("badges")
        val receivedAt = tag("tmi-sent-ts")?.toLongOrNull()?.let(Instant::ofEpochMilli) ?: Instant.now()

        messageTracker.enqueue(
            ChatMessageRecord(
                channelUuid = info.uuid,
                channelId = channel.id,
                channelName = channel.name,
                userId = user.id,
                userName = user.name,
                displayName = tag("display-name"),
                isMod = tag("mod") == "1" || badges?.contains("moderator/") == true,
                isSubscriber = tag("subscriber") == "1",
                isVip = tag("vip") == "1" || badges?.contains("vip/") == true,
                isBroadcaster = badges?.contains("broadcaster/") == true,
                isTurbo = tag("turbo") == "1",
                isFirstMessage = tag("first-msg") == "1",
                isAction = isAction,
                badges = badges,
                badgeInfo = tag("badge-info"),
                color = tag("color")?.takeIf { it.isNotBlank() },
                bits = tag("bits")?.toIntOrNull() ?: 0,
                content = message,
                emotes = tag("emotes")?.takeIf { it.isNotBlank() },
                replyToMessageId = tag("reply-parent-msg-id"),
                messageId = tag("id") ?: UUID.randomUUID().toString(),
                receivedAt = receivedAt,
            )
        )
        userTracker.record(user.id, user.name, tag("display-name"), receivedAt)
    }
}
