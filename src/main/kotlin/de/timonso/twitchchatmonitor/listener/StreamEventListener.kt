package de.timonso.twitchchatmonitor.listener

import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent
import com.github.twitch4j.chat.events.channel.RaidEvent
import com.github.twitch4j.chat.events.channel.SubscriptionEvent
import com.github.twitch4j.common.events.domain.EventChannel
import de.timonso.twitchchatmonitor.database.ChannelRegistry
import de.timonso.twitchchatmonitor.tracker.EventTracker
import de.timonso.twitchchatmonitor.tracker.StreamEventRecord
import org.slf4j.LoggerFactory
import java.time.Instant

class StreamEventListener(private val tracker: EventTracker) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun register(eventManager: EventManager) {
        eventManager.onEvent(SubscriptionEvent::class.java) { event ->
            val gifted = event.gifted == true
            val eventType = when {
                gifted -> "sub_gift"
                (event.months ?: 1) > 1 -> "resub"
                else -> "sub"
            }
            record(
                channel = event.channel,
                eventType = eventType,
                userId = event.user?.id,
                userName = event.user?.name,
                giftedByName = event.giftedBy?.name,
                subTier = event.subscriptionPlan,
                months = event.months,
                message = event.message?.orElse(null),
            )
        }
        eventManager.onEvent(GiftSubscriptionsEvent::class.java) { event ->
            record(
                channel = event.channel,
                eventType = "community_gift",
                userId = event.user?.id,
                userName = event.user?.name,
                subTier = event.tier?.ordinalName(),
                giftCount = event.count,
            )
        }
        eventManager.onEvent(RaidEvent::class.java) { event ->
            record(
                channel = event.channel,
                eventType = "raid",
                userId = event.raider?.id,
                userName = event.raider?.name,
                viewerCount = event.viewers,
            )
        }
        eventManager.onEvent(ModAnnouncementEvent::class.java) { event ->
            record(
                channel = event.channel,
                eventType = "announcement",
                userId = event.announcer?.id,
                userName = event.announcer?.name,
                message = event.message,
            )
        }
    }

    private fun record(
        channel: EventChannel,
        eventType: String,
        userId: String? = null,
        userName: String? = null,
        giftedByName: String? = null,
        subTier: String? = null,
        months: Int? = null,
        giftCount: Int? = null,
        viewerCount: Int? = null,
        message: String? = null,
    ) {
        val info = ChannelRegistry.byBroadcasterId(channel.id)
        if (info == null) {
            log.warn("Event aus unbekanntem Channel {} ({}) wird verworfen", channel.name, channel.id)
            return
        }
        tracker.enqueue(
            StreamEventRecord(
                channelUuid = info.uuid,
                channelId = channel.id,
                channelName = channel.name,
                eventType = eventType,
                userId = userId,
                userName = userName,
                giftedByName = giftedByName,
                subTier = subTier,
                months = months,
                giftCount = giftCount,
                viewerCount = viewerCount,
                message = message,
                occurredAt = Instant.now(),
            )
        )
    }
}
