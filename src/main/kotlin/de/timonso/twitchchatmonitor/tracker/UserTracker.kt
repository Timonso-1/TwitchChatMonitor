package de.timonso.twitchchatmonitor.tracker

import de.timonso.twitchchatmonitor.database.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UserTracker(private val flushIntervalSeconds: Long) {
    private val log = LoggerFactory.getLogger(javaClass)

    private data class Activity(
        val userName: String,
        val displayName: String?,
        val count: Long,
        val lastSeenAt: Instant,
    )

    private val pending = ConcurrentHashMap<String, Activity>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "user-tracker").apply { isDaemon = true }
    }

    fun start() {
        scheduler.scheduleWithFixedDelay(::flush, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS)
    }

    fun record(userId: String, userName: String, displayName: String?, seenAt: Instant) {
        pending.merge(userId, Activity(userName, displayName, 1, seenAt)) { old, new ->
            Activity(
                userName = new.userName,
                displayName = new.displayName ?: old.displayName,
                count = old.count + 1,
                lastSeenAt = maxOf(old.lastSeenAt, new.lastSeenAt),
            )
        }
    }

    private fun flush() {
        if (pending.isEmpty()) return
        val snapshot = HashMap<String, Activity>()
        for (key in pending.keys.toList()) {
            pending.remove(key)?.let { snapshot[key] = it }
        }
        try {
            transaction {
                snapshot.forEach { (userId, activity) ->
                    val existing = Users.selectAll()
                        .where { Users.userId eq userId }
                        .limit(1)
                        .firstOrNull()
                    if (existing == null) {
                        Users.insert {
                            it[Users.userId] = userId
                            it[userName] = activity.userName
                            it[displayName] = activity.displayName
                            it[firstSeenAt] = activity.lastSeenAt
                            it[lastSeenAt] = activity.lastSeenAt
                            it[messageCount] = activity.count
                        }
                    } else {
                        Users.update({ Users.userId eq userId }) {
                            it[userName] = activity.userName
                            it[displayName] = activity.displayName ?: existing[Users.displayName]
                            it[lastSeenAt] = activity.lastSeenAt
                            it[messageCount] = existing[Users.messageCount] + activity.count
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Konnte User-Statistiken nicht speichern", e)
        }
    }

    fun stop() {
        scheduler.shutdownNow()
        flush()
    }
}
