package de.timonso.twitchchatmonitor.twitch

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder

object TwitchClientFactory {

    fun create(accessToken: String): TwitchClient {
        val credential = OAuth2Credential("twitch", accessToken)
        return TwitchClientBuilder.builder()
            .withEnableChat(true)
            .withChatAccount(credential)
            .build()
    }
}
