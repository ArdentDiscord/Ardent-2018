package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class Unmute : Command("unmute", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.message.mentionedUsers.isEmpty()) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                translate("unmute.mention", event, register), this, event)
        else {
            val mutes = register.database.getMutes().filter { it.guildId == event.guild.id }
            event.message.mentionedUsers.forEach { toUnmute ->
                mutes.firstOrNull { it.muted == toUnmute.id }?.let {
                    val muteRoleId = register.database.getGuildData(event.guild).muteRoleId
                    muteRoleId?.let { event.guild.getRoleById(muteRoleId) }?.let { muteRole ->
                        event.guild.removeRoleFromMember(event.guild.getMember(toUnmute)!!, muteRole)
                                .reason("Unmuted").queue({ _ ->
                                    register.database.delete(it, false)
                                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                            translate("unmute.unmuted", event, register).apply(toUnmute.display()), this, event)
                                }, { _ ->
                                    register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                            translate("unmute.no_unmute", event, register).apply(toUnmute.display()), this, event)
                                })
                    }
                } ?: register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        translate("unmute.not_muted", event, register).apply(toUnmute.display()), this, event)
            }
        }
    }

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_ROLES))
}