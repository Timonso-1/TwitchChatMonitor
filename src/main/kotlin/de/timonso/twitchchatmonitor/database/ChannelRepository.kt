package de.timonso.twitchchatmonitor.database

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ChannelInfo(
    val uuid: String,
    val broadcasterId: String,
    val name: String,
    val active: Boolean,
)

object ChannelRepository {

    fun loadAll(): List<ChannelInfo> = transaction {
        Channels.selectAll().map {
            ChannelInfo(
                uuid = it[Channels.uuid],
                broadcasterId = it[Channels.broadcasterId],
                name = it[Channels.name],
                active = it[Channels.active],
            )
        }
    }

    fun insertIfMissing(broadcasterId: String, name: String, createdByName: String? = null): Boolean = transaction {
        val exists = Channels.selectAll()
            .where { Channels.broadcasterId eq broadcasterId }
            .limit(1)
            .any()
        if (exists) {
            false
        } else {
            val now = Instant.now()
            Channels.insert {
                it[uuid] = UUID.randomUUID().toString()
                it[Channels.broadcasterId] = broadcasterId
                it[Channels.name] = name
                it[active] = true
                it[createdAt] = now
                it[updatedAt] = now
                it[Channels.createdByName] = createdByName
            }
            true
        }
    }
}

object ChannelRegistry {
    private val byBroadcasterId = ConcurrentHashMap<String, ChannelInfo>()

    fun update(channels: List<ChannelInfo>) {
        val ids = channels.map { it.broadcasterId }.toSet()
        byBroadcasterId.keys.retainAll(ids)
        channels.forEach { byBroadcasterId[it.broadcasterId] = it }
    }

    fun byBroadcasterId(id: String): ChannelInfo? = byBroadcasterId[id]
}
