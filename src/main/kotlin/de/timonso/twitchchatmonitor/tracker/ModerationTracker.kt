package de.timonso.twitchchatmonitor.tracker

import de.timonso.twitchchatmonitor.database.ModerationActions
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class ModerationRecord(
    val channelUuid: String,
    val channelId: String,
    val channelName: String,
    val action: String,
    val targetUserId: String?,
    val targetUserName: String?,
    val durationSeconds: Int?,
    val reason: String?,
    val deletedMessageId: String?,
    val deletedMessageContent: String?,
    val occurredAt: Instant,
)

class ModerationTracker(
    batchSize: Int,
    flushIntervalMs: Long,
) : BatchTracker<ModerationRecord>("moderation-tracker", batchSize, flushIntervalMs) {

    override fun persist(batch: List<ModerationRecord>) {
        transaction {
            ModerationActions.batchInsert(batch, shouldReturnGeneratedValues = false) { r ->
                this[ModerationActions.channelUuid] = r.channelUuid
                this[ModerationActions.channelId] = r.channelId
                this[ModerationActions.channelName] = r.channelName
                this[ModerationActions.action] = r.action
                this[ModerationActions.targetUserId] = r.targetUserId
                this[ModerationActions.targetUserName] = r.targetUserName
                this[ModerationActions.durationSeconds] = r.durationSeconds
                this[ModerationActions.reason] = r.reason
                this[ModerationActions.deletedMessageId] = r.deletedMessageId
                this[ModerationActions.deletedMessageContent] = r.deletedMessageContent
                this[ModerationActions.occurredAt] = r.occurredAt
            }
        }
    }
}
