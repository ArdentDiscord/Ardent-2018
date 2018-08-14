package com.ardentbot.commands.info

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.selectFromList
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.Instant
import java.time.ZoneOffset

@ModuleMapping("info")
class UserInfo : Command("whois",
        arrayOf("userinfo", "uinfo", "ui"), 10) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        event.message.mentionedUsers.getOrNull(0)?.let { doCommand(it, event, arguments, flags, register) } ?: {
            if (arguments.isEmpty()) doCommand(null, event, arguments, flags, register)
            else {
                val query = arguments.concat()
                val users = event.guild.members.filter { it.effectiveName.contains(query, true) || it.user.name.contains(query, true) }
                when {
                    users.isEmpty() -> doCommand(null, event, arguments, flags, register)
                    users.size > 9 -> event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                            "10 or more users containing this query were found. Please be more specific or tag them", register)
                    else -> event.channel.selectFromList(event.member, "Select which user you want", users.map { it.asMention }, { i, _ ->
                        doCommand(users[i].user, event, arguments, flags, register)
                    }, register = register)
                }
            }
        }.invoke()
    }

    private fun doCommand(mentioned: User?, event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (mentioned == null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    "You need to specify the name of a user or mention them!", this, event)
            return
        }

        val member = event.guild.getMember(mentioned)
        val embed = getEmbed("[] | User Information".apply(member.effectiveName), event.author, event.guild)
                .setThumbnail(mentioned.avatarUrl)
                .addField("Name", mentioned.display(), true)
                .addField("Nickname", member.nickname ?: "None", true)
                .addField("Status", member.onlineStatus.name, true)
                .addField("Server Join Date", member.joinDate.toLocalDate().toString(), true)
                .addField("Days in Guild",
                        ((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                                member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond()) / (60 * 60 * 24))
                                .toString(), true)
                .addField("Roles", member.roles.map { it.name }.joinToString(), true)
                .addField("Account Creation Date", mentioned.creationTime.toLocalDate().toString(), true)

        register.sender.cmdSend(embed, this, event)
    }
}