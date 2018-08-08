package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.Instant
import java.time.ZoneOffset

@ModuleMapping("info")
class UserInfo : Command("whois",
        arrayOf("userinfo", "uinfo", "ui"), 10) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val mentioned = event.message.mentionedUsers.getOrNull(0)
        if (mentioned == null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    "You need to mention a user!", this, event)
            return
        }
        val member = event.guild.getMember(mentioned)
        val embed = getEmbed(" | User Information".apply(member.effectiveName), event.author, event.guild)
                .setThumbnail(mentioned.avatarUrl)
                .addField("Name", mentioned.display(), true)
                .addField("Nickname", member.nickname ?: "None", true)
                .addField("Status", member.onlineStatus.key, true)
                .addField("Server Join Date", member.joinDate.toLocalDate().toString(), true)
                .addField("Days in Guild", Math.ceil((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                        event.member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond() / (60 * 60 * 24)).toDouble())
                        .toString(), true)
                .addField("Roles", member.roles.map { it.name }.joinToString(), true)
                .addField("Account Creation Date", mentioned.creationTime.toLocalDate().toString(), true)

        register.sender.cmdSend(embed, this, event)
    }
}