package com.ardentbot.kotlin

import com.ardentbot.core.utils.IllegalArgumentApplicationException
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.net.URLEncoder

fun User.display() = "$name#$discriminator"

fun String.remove(other: String, ignoreCase: Boolean = true): String {
    return split(" ").filter { !it.equals(other, ignoreCase) }.concat()
}

fun String.encode(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20").replace("%2F", "_")

fun String.apply(vararg parameters: Any): String = apply(parameters.map { it.toString() })

fun String.apply(parameters: List<String>): String {
    val indices = mutableListOf<Int>()
    for (i in 0..(this.length - 2)) if (this[i] == '[' && this[i + 1] == ']') indices.add(i)
    if (parameters.size != indices.size) throw IllegalArgumentApplicationException("${indices.size} indices were found. " +
            "Expected the same amount of parameters. Found: ${parameters.size}")
    val applied = StringBuilder(this)
    indices.forEachIndexed { index, location ->
        val start = location + (applied.length - this.length)
        applied.replace(start, start + 2, parameters[index])
    }
    return applied.toString()
}

infix fun String.shortenTo(toLength: Int): String {
    return if (length <= toLength) this else this.substring(0, toLength - 3) + "..."
}

fun String.toChannelId() = removePrefix("<#").removeSuffix(">")
fun String.toUserId() = removePrefix("<@").removePrefix("!").removeSuffix(">")

fun String.toChannel(guild: Guild) = toLongOrNull()?.let { guild.getTextChannelById(it) }
fun String.toMember(guild: Guild) = toLongOrNull()?.let { guild.getMemberById(it) }