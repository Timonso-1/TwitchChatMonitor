package de.timonso.twitchchatmonitor.tracker

import de.timonso.twitchchatmonitor.database.Messages
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class ChatMessageRecord(
    val channelUuid: String,
    val channelId: String,
    val channelName: String,
    val userId: String,
    val userName: String,
    val displayName: String?,
    val isMod: Boolean,
    val isSubscriber: Boolean,
    val isVip: Boolean,
    val isBroadcaster: Boolean,
    val isTurbo: Boolean,
    val isFirstMessage: Boolean,
    val isAction: Boolean,
    val badges: String?,
    val badgeInfo: String?,
    val color: String?,
    val bits: Int,
    val content: String,
    val emotes: String?,
    val replyToMessageId: String?,
    val messageId: String,
    val receivedAt: Instant,
)

class MessageTracker(
    batchSize: Int,
    flushIntervalMs: Long,
) : BatchTracker<ChatMessageRecord>("message-tracker", batchSize, flushIntervalMs) {

    override fun persist(batch: List<ChatMessageRecord>) {
        transaction {
            Messages.batchInsert(batch, ignore = true, shouldReturnGeneratedValues = false) { r ->
                this[Messages.channelUuid] = r.channelUuid
                this[Messages.channelId] = r.channelId
                this[Messages.channelName] = r.channelName
                this[Messages.userId] = r.userId
                this[Messages.userName] = r.userName
                this[Messages.displayName] = r.displayName
                this[Messages.isMod] = r.isMod
                this[Messages.isSubscriber] = r.isSubscriber
                this[Messages.isVip] = r.isVip
                this[Messages.isBroadcaster] = r.isBroadcaster
                this[Messages.isTurbo] = r.isTurbo
                this[Messages.isFirstMessage] = r.isFirstMessage
                this[Messages.isAction] = r.isAction
                this[Messages.badges] = r.badges
                this[Messages.badgeInfo] = r.badgeInfo
                this[Messages.color] = r.color
                this[Messages.bits] = r.bits
                this[Messages.content] = r.content
                this[Messages.emotes] = r.emotes
                this[Messages.replyToMessageId] = r.replyToMessageId
                this[Messages.messageId] = r.messageId
                this[Messages.receivedAt] = r.receivedAt
            }
        }
    }
}
