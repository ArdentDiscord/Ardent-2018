package com.ardentbot.core

import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException

class EventMessageSender {
    companion object {
        fun joinMessage(event: GuildMemberJoinEvent, register: ArdentRegister) {
            val data = register.database.getGuildData(event.guild)
            val channel = data.joinMessage.channelId?.let { event.guild.getTextChannelById(it) }
            if (data.joinMessage.message != null) {
                event.user.openPrivateChannel().queue {
                    it.sendMessage("**Message from ${event.guild.name}**: ${data.joinMessage.message}").queue()
                }
                if (channel != null) {
                    channel.sendMessage(data.joinMessage.message!!
                            .replace("[user]", event.member.asMention)
                            .replace("[serversize]", event.guild.members.size.toString())
                            .replace("[server]", event.guild.name)
                    ).queue()
                }
            }
            val defaultRole = data.defaultRoleId?.let { event.guild.getRoleById(it) } ?: return
            try {
                event.guild.controller.addSingleRoleToMember(event.member, defaultRole).reason("Ardent default role")
                        .queue()
            } catch (e: InsufficientPermissionException) {
                event.guild.owner.user.openPrivateChannel().queue {
                    it.sendMessage(register.translationManager.translate("sender.default_role_fail", data.language
                            ?: Language.ENGLISH).apply(event.guild.name)).queue()
                }
            }
        }

        fun leaveMessage(event: GuildMemberLeaveEvent, register: ArdentRegister) {
            val data = register.database.getGuildData(event.guild)
            val channel = data.leaveMessage.channelId?.let { event.guild.getTextChannelById(it) }
            if (channel != null && data.leaveMessage.message != null) {
                channel.sendMessage(data.leaveMessage.message!!
                        .replace("[user]", "**${event.user.display()}**")
                        .replace("[serversize]", event.guild.members.size.toString())
                        .replace("[server]", event.guild.name)
                ).queue()
            }
        }
    }
}