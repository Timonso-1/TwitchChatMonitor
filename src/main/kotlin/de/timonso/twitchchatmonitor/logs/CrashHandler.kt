package de.timonso.twitchchatmonitor.logs

import org.slf4j.LoggerFactory

object CrashHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            log.error("Unbehandelte Exception in Thread '{}'", thread.name, e)
        }
    }
}
