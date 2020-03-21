package com.ardentbot.kotlin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.commands.Command
import com.ardentbot.core.selectFromList
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

fun getUser(query: String, event: GuildMessageReceivedEvent, command: Command, register: ArdentRegister, callback: (User?) -> Unit) {
    val users = if (event.message.mentionedUsers.any { it.id != event.guild.selfMember.user.id }) {
        event.message.mentionedUsers.filter { it.id != event.guild.selfMember.user.id }
    } else event.guild.members.asSequence().filter { it.effectiveName.contains(query, true) || it.user.name.contains(query, true) }
            .map { it.user }.toList()

    when {
        users.isEmpty() -> callback(null)
        users.size == 1 -> callback(users[0])
        users.size > 9 -> event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                command.translate("userinfo.too_broad", event, register), register)
        else -> event.channel.selectFromList(event.member!!, command.translate("userinfo.select_user", event, register),
                users.map { it.asMention }, { i, _ ->
            callback(users[i])
        }, failure = { callback(null) }, register = register)
    }
}