package de.timonso.twitchchatmonitor.tracker

import de.timonso.twitchchatmonitor.database.Events
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class StreamEventRecord(
    val channelUuid: String,
    val channelId: String,
    val channelName: String,
    val eventType: String,
    val userId: String?,
    val userName: String?,
    val giftedByName: String?,
    val subTier: String?,
    val months: Int?,
    val giftCount: Int?,
    val viewerCount: Int?,
    val message: String?,
    val occurredAt: Instant,
)

class EventTracker(
    batchSize: Int,
    flushIntervalMs: Long,
) : BatchTracker<StreamEventRecord>("event-tracker", batchSize, flushIntervalMs) {

    override fun persist(batch: List<StreamEventRecord>) {
        transaction {
            Events.batchInsert(batch, shouldReturnGeneratedValues = false) { r ->
                this[Events.channelUuid] = r.channelUuid
                this[Events.channelId] = r.channelId
                this[Events.channelName] = r.channelName
                this[Events.eventType] = r.eventType
                this[Events.userId] = r.userId
                this[Events.userName] = r.userName
                this[Events.giftedByName] = r.giftedByName
                this[Events.subTier] = r.subTier
                this[Events.months] = r.months
                this[Events.giftCount] = r.giftCount
                this[Events.viewerCount] = r.viewerCount
                this[Events.message] = r.message
                this[Events.occurredAt] = r.occurredAt
            }
        }
    }
}
