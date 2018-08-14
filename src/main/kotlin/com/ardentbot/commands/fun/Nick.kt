package com.ardentbot.commands.`fun`

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("fun")
class Nick : Command("nick", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a nickname", register)
        else if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.NICKNAME_CHANGE)), event, arguments, flags, register)) {
            try {
                event.guild.controller.setNickname(event.member, arguments.concat()).reason("/nick - changed from [] to []"
                        .apply(event.member.nickname ?: "None", arguments.concat())).queue {
                    event.message.delete().reason("/nick trigger").queue()
                }
            } catch (e: Exception) {
                event.channel.send(Emojis.THINKING_FACE.cmd + translate("permission", event, register)
                        .apply(translate("nick.permission", event, register)), register)
            }
        }
    }
}