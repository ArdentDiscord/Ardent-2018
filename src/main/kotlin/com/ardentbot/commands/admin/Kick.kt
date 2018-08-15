package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class Kick : Command("kick", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.message.mentionedUsers.isEmpty()) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                translate("kick.mention", event, register), this, event)
        // Doesn't use the getUser function - a user could accidentally select the wrong person, which would not be good
        else {
            event.message.mentionedUsers.map { event.guild.getMember(it) }.forEach { mentioned ->
                if (mentioned.hasPermission(Permission.KICK_MEMBERS)) {
                    register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("kick.cannot_kick", event, register), this, event)
                } else {
                    try {
                        event.guild.controller.kick(mentioned).reason("Kicked by ${event.author.display()}").queue {
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    translate("kick.response", event, register).apply("**${mentioned.user.display()}**"), this, event)
                        }
                    } catch (e: Exception) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                translate("kick.no_permission", event, register).apply(mentioned.user.display()), this, event)
                    }
                }
            }
        }
    }

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.KICK_MEMBERS))
}