package de.timonso.twitchchatmonitor.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.timonso.twitchchatmonitor.config.DbConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(javaClass)

    fun init(cfg: DbConfig) {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${cfg.host}:${cfg.port}/${cfg.database}" +
                    "?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Europe/Berlin"
            username = cfg.user
            password = cfg.password
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = 5
            poolName = "twitch-chat-monitor"
        })
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(Channels, Messages, ModerationActions, Events, Users)
        }
        log.info("Datenbankverbindung zu {}:{}/{} hergestellt", cfg.host, cfg.port, cfg.database)
    }
}
