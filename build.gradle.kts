plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    application
    id("com.gradleup.shadow") version "9.6.0"
}

group = "de.timonso"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Twitch
    implementation("com.github.twitch4j:twitch4j:1.25.0")

    // Datenbank (MySQL + Connection Pool + Exposed ORM)
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("com.mysql:mysql-connector-j:9.3.0")

    // Config (.env + config.json)
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("de.timonso.twitchchatmonitor.TwitchChatMonitorKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveFileName.set("TwitchChatMonitor.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}
