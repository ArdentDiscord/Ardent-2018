package com.ardentbot.web

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.commands.Command
import com.google.gson.Gson
import org.jsoup.Jsoup

val dapi = "https://discordapp.com/api"
lateinit var base:String
lateinit var loginRedirect: String

private val gson = Gson()

data class CommandWrapper(val categoryName: String, val categoryId: String, val commands: List<Command>)

data class Failure(val failure: String)

class Setting(val route: String, val name: String, val description: String, vararg val optionalParameters: String) {
    override fun toString(): String {
        return gson.toJson(this)
    }
}

data class Token(val access_token: String, val token_type: String, val expires_in: Int, val refresh_token: String, val scope: String)
data class IdentificationObject(val username: String, val verified: Boolean, val mfa_enabled: Boolean, val id: String, val avatar: String, val discriminator: String)

enum class Scope(val route: String) {
    CONNECTIONS("/users/@me/connections"),
    EMAIL("/users/@me"),
    IDENTIFY("/users/@me"),
    GUILDS("/users/@me/guilds"),
    BOT_INFORMATION("/oauth2/applications/@me");

    override fun toString(): String {
        return route
    }
}

fun identityObject(access_token: String): IdentificationObject? {
    val obj = gson.fromJson(retrieveObject(access_token, Scope.IDENTIFY), IdentificationObject::class.java)
    return if (obj.id == null) null /* This is possible due to issues with the kotlin compiler */
    else obj
}

fun retrieveObject(access_token: String, scope: Scope): String {
    return Jsoup.connect("$dapi$scope").ignoreContentType(true).ignoreHttpErrors(true)
            .header("authorization", "Bearer $access_token")
            .header("cache-control", "no-cache")
            .get()
            .text()
}

fun retrieveToken(code: String, register: ArdentRegister): Token? {
    val response = Jsoup.connect("$dapi/oauth2/token").ignoreContentType(true).ignoreHttpErrors(true)
            .header("content-type", "application/x-www-form-urlencoded")
            .header("authorization", "Bearer $code")
            .header("cache-control", "no-cache")
            .data("client_id", register.selfUser.id)
            .data("client_secret", register.config["client_secret"])
            .data("grant_type", "authorization_code")
            .data("redirect_uri", loginRedirect)
            .data("code", code)
            .post()
    val data = gson.fromJson(response.text(), Token::class.java)
    return if (data.access_token == null) null // this is a possibility due to issues with the kotlin compiler
    else data /* verified non null object */
}