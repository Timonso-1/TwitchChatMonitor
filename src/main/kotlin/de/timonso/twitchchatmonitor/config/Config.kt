package de.timonso.twitchchatmonitor.config

import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SeedChannel(
    val name: String,
    val twitchId: String,
)

@Serializable
data class Settings(
    val seedChannels: List<SeedChannel> = emptyList(),
    val channelSyncIntervalSeconds: Long = 60,
    val messageBatchSize: Int = 100,
    val messageFlushIntervalMs: Long = 1000,
    val userFlushIntervalSeconds: Long = 5,
)

data class DbConfig(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
)

data class Config(
    val twitchAccessToken: String,
    val db: DbConfig,
    val settings: Settings,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): Config {
            val env = dotenv {
                ignoreIfMissing = true
            }

            fun required(key: String): String =
                env[key]?.takeIf { it.isNotBlank() }
                    ?: error("Fehlende Umgebungsvariable '$key', bitte in der .env setzen (siehe .env.example)")

            val settingsJson = File("config.json").takeIf { it.exists() }?.readText()
                ?: Config::class.java.getResource("/config.json")?.readText()
                ?: "{}"

            return Config(
                twitchAccessToken = required("TWITCH_ACCESS_TOKEN"),
                db = DbConfig(
                    host = env["DB_HOST"] ?: "localhost",
                    port = (env["DB_PORT"] ?: "3306").toInt(),
                    database = required("DB_NAME"),
                    user = required("DB_USER"),
                    password = required("DB_PASSWORD"),
                ),
                settings = json.decodeFromString(settingsJson),
            )
        }
    }
}
