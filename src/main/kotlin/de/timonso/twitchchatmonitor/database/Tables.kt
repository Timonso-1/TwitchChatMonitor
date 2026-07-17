package de.timonso.twitchchatmonitor.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Channels : Table("channels") {
    val uuid = char("uuid", 36)
    val broadcasterId = varchar("broadcaster_id", 32).uniqueIndex()
    val name = varchar("name", 64)
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val createdByUuid = char("created_by_uuid", 36).nullable()
    val createdByName = varchar("created_by_name", 64).nullable()

    override val primaryKey = PrimaryKey(uuid)
}

object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val channelUuid = char("channel_uuid", 36).references(Channels.uuid)
    val channelId = varchar("channel_id", 32)
    val channelName = varchar("channel_name", 64)
    val userId = varchar("user_id", 32)
    val userName = varchar("user_name", 64)
    val displayName = varchar("display_name", 64).nullable()
    val isMod = bool("is_mod").default(false)
    val isSubscriber = bool("is_subscriber").default(false)
    val isVip = bool("is_vip").default(false)
    val isBroadcaster = bool("is_broadcaster").default(false)
    val isTurbo = bool("is_turbo").default(false)
    val isFirstMessage = bool("is_first_message").default(false)
    val isAction = bool("is_action").default(false)
    val badges = varchar("badges", 512).nullable()
    val badgeInfo = varchar("badge_info", 255).nullable()
    val color = varchar("color", 16).nullable()
    val bits = integer("bits").default(0)
    val content = text("content")
    val emotes = text("emotes").nullable()
    val replyToMessageId = varchar("reply_to_message_id", 64).nullable()
    val messageId = varchar("message_id", 64).uniqueIndex()
    val receivedAt = timestamp("received_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, channelUuid, receivedAt)
        index(isUnique = false, userId, receivedAt)
    }
}

object ModerationActions : Table("moderation_actions") {
    val id = long("id").autoIncrement()
    val channelUuid = char("channel_uuid", 36).references(Channels.uuid)
    val channelId = varchar("channel_id", 32)
    val channelName = varchar("channel_name", 64)
    val action = varchar("action", 16)
    val targetUserId = varchar("target_user_id", 32).nullable()
    val targetUserName = varchar("target_user_name", 64).nullable()
    val durationSeconds = integer("duration_seconds").nullable()
    val reason = varchar("reason", 500).nullable()
    val deletedMessageId = varchar("deleted_message_id", 64).nullable()
    val deletedMessageContent = text("deleted_message_content").nullable()
    val occurredAt = timestamp("occurred_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, channelUuid, occurredAt)
    }
}

object Events : Table("events") {
    val id = long("id").autoIncrement()
    val channelUuid = char("channel_uuid", 36).references(Channels.uuid)
    val channelId = varchar("channel_id", 32)
    val channelName = varchar("channel_name", 64)
    val eventType = varchar("event_type", 24)
    val userId = varchar("user_id", 32).nullable()
    val userName = varchar("user_name", 64).nullable()
    val giftedByName = varchar("gifted_by_name", 64).nullable()
    val subTier = varchar("sub_tier", 16).nullable()
    val months = integer("months").nullable()
    val giftCount = integer("gift_count").nullable()
    val viewerCount = integer("viewer_count").nullable()
    val message = text("message").nullable()
    val occurredAt = timestamp("occurred_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, channelUuid, occurredAt)
        index(isUnique = false, eventType, occurredAt)
    }
}

object Users : Table("users") {
    val userId = varchar("user_id", 32)
    val userName = varchar("user_name", 64)
    val displayName = varchar("display_name", 64).nullable()
    val firstSeenAt = timestamp("first_seen_at")
    val lastSeenAt = timestamp("last_seen_at")
    val messageCount = long("message_count").default(0)

    override val primaryKey = PrimaryKey(userId)
}
