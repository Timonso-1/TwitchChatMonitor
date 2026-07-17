# TwitchChatMonitor

Bot, der Twitch-Chats mitliest und Nachrichten, Moderationsaktionen und Events in einer MySQL-Datenbank speichert.
Channels werden über die Datenbank verwaltet (vorbereitet für ein späteres Web-Panel).

## Setup

1. `.env.example` nach `.env` kopieren und ausfüllen:
   - `TWITCH_ACCESS_TOKEN`: OAuth-Token mit `chat:read` Scope (z.B. über https://twitchtokengenerator.com)
   - `DB_*`: MySQL-Zugangsdaten
2. MySQL-Datenbank anlegen (utf8mb4, damit Emojis funktionieren):
   ```sql
   CREATE DATABASE twitch_chat_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   Die Tabellen legt der Bot beim Start selbst an.
3. In `config.json` unter `seedChannels` die ersten Channels mit Name und Twitch-ID eintragen,
   sie werden beim Start automatisch in die Datenbank übernommen:
   ```json
   {
     "seedChannels": [
       { "name": "beispielchannel", "twitchId": "12345678" }
     ]
   }
   ```
   Die Twitch-ID eines Channels findet man z.B. über https://www.streamweasels.com/tools/convert-twitch-username-to-user-id/
4. Starten: `./gradlew run`

## Konfiguration

| Datei | Zweck |
|---|---|
| `.env` | Secrets (Token, DB-Zugangsdaten), nicht im Git |
| `config.json` | Bot-Einstellungen; eine `config.json` im Arbeitsverzeichnis überschreibt die gebundelten Defaults |
| `logs/` | Rotierende Logfiles (`bot.log` alles, `error.log` nur Warnungen, Fehler und Crashes) |

## Datenbank-Schema

**channels**: über UUID intern verwaltet, `active` steuert, ob der Bot den Chat joint.
Wird regelmäßig abgeglichen, Änderungen greifen also ohne Neustart.

`uuid (PK)`, `broadcaster_id (unique)`, `name`, `active`, `created_at`, `updated_at`,
`created_by_uuid`, `created_by_name`

**messages**: eine Zeile pro Chat-Nachricht.

`id (PK)`, `channel_uuid (FK)`, `channel_id`, `channel_name`, `user_id`, `user_name`,
`display_name`, `is_mod`, `is_subscriber`, `is_vip`, `is_broadcaster`, `is_turbo`,
`is_first_message`, `is_action` (/me), `badges` (Rohformat, z.B. `moderator/1,subscriber/6`),
`badge_info` (u.a. genaue Sub-Monate), `color`, `bits` (Cheers), `content`, `emotes`,
`reply_to_message_id`, `message_id (unique)`, `received_at`

**moderation_actions**: Timeouts, Bans, gelöschte Nachrichten und Chat-Clears.

`id (PK)`, `channel_uuid (FK)`, `channel_id`, `channel_name`, `action` (timeout/ban/delete/clear_chat),
`target_user_id`, `target_user_name`, `duration_seconds`, `reason`, `deleted_message_id`,
`deleted_message_content`, `occurred_at`

**events**: Subs, Resubs, Gift-Subs, Raids und Announcements.

`id (PK)`, `channel_uuid (FK)`, `channel_id`, `channel_name`,
`event_type` (sub/resub/sub_gift/community_gift/raid/announcement), `user_id`, `user_name`,
`gifted_by_name`, `sub_tier`, `months`, `gift_count`, `viewer_count`, `message`, `occurred_at`

**users**: aggregierte Statistik pro User, spart dem Panel teure Abfragen über die messages-Tabelle.

`user_id (PK)`, `user_name`, `display_name`, `first_seen_at`, `last_seen_at`, `message_count`

Indizes: `(channel_uuid, received_at)` bzw. `(channel_uuid, occurred_at)` und `(user_id, received_at)`
für Panel-Abfragen, `message_id` unique als Duplikat-Schutz (z.B. bei Reconnects).

## Struktur

| Paket | Inhalt |
|---|---|
| `config` | Laden von `.env` und `config.json` |
| `database` | Tabellen, Verbindungsaufbau, Channel-Repository und -Registry |
| `listener` | Twitch-Event-Handler (Chat, Moderation, Stream-Events) |
| `tracker` | Persistierung: Batch-Inserts über Queues, User-Statistik |
| `twitch` | Twitch-Client, Channel-Seeding und Channel-Sync |
| `logs` | Crash-Handler, schreibt unbehandelte Exceptions ins Error-Log |

## Deployment auf Pterodactyl

1. Fat-Jar bauen: `./gradlew shadowJar` (Ergebnis: `build/libs/TwitchChatMonitor.jar`)
2. Im Panel einen Server mit einem generischen Java-Egg anlegen
   (z.B. "Generic Java" aus den parkervcp Community-Eggs), Docker-Image `java_21`
3. Per Dateimanager oder SFTP ins Server-Verzeichnis hochladen:
   - `TwitchChatMonitor.jar`
   - `.env` (mit den echten Zugangsdaten)
   - optional `config.json` (überschreibt die im Jar gebundelten Defaults)
4. Startup-Befehl: `java -Xms128M -XX:MaxRAMPercentage=95.0 -jar TwitchChatMonitor.jar`
5. Der `logs/` Ordner wird beim ersten Start automatisch angelegt

Die MySQL-Datenbank muss vom Pterodactyl-Node aus erreichbar sein.

## Ideen für später

- Partitionierung der `messages`-Tabelle nach `received_at` (monatlich), sobald sie sehr groß wird
