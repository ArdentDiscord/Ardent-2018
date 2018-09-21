package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.Instant
import java.time.ZoneOffset

@ModuleMapping("info")
class UserInfo : Command("whois", arrayOf("userinfo", "uinfo", "ui"), 10) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        event.message.mentionedUsers.getOrNull(0)?.let { doCommand(it, event, register) } ?: {
            if (arguments.isEmpty()) doCommand(null, event, register)
            else {
                val query = arguments.concat()
                getUser(query, event, this, register) { user -> doCommand(user, event, register) }
            }
        }.invoke()
    }

    private fun doCommand(mentioned: User?, event: GuildMessageReceivedEvent, register: ArdentRegister) {
        if (mentioned == null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    translate("general.specify_or_mention_user", event, register), this, event)
            return
        }

        val member = event.guild.getMember(mentioned)
        val embed = getEmbed(translate("userinfo.embed_title", event, register).apply(member.effectiveName), event.author, event.guild)
                .setThumbnail(mentioned.avatarUrl)
                .addField(translate("userinfo.name", event, register), mentioned.display(), true)
                .addField(translate("userinfo.nickname", event, register), member.nickname
                        ?: translate("general.none", event, register), true)
                .addField(translate("userinfo.status", event, register), member.onlineStatus.key, true)
                .addField(translate("userinfo.join_date", event, register), member.joinDate.toLocalDate().toString(), true)
                .addField(translate("userinfo.days_in_guild", event, register),
                        ((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                                member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond()) / (60 * 60 * 24))
                                .toString(), true)
                .addField(translate("general.roles", event, register), member.roles.map { it.name }.joinToString(), true)
                .addField(translate("userinfo.account_creation_date", event, register), mentioned.creationTime.toLocalDate().toString(), true)

        register.sender.cmdSend(embed, this, event)
    }
}