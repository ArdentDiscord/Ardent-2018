package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class GetId : Command("getid", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        event.message.mentionedUsers.take(5).forEach {
            register.sender.cmdSend(translate("getid.response", event, register)
                    .apply(it.display(), it.id), this, event)
        }
        event.message.mentionedRoles.take(5).forEach {
            register.sender.cmdSend(translate("getid.response", event, register)
                    .apply(it.asMention, it.id), this, event)
        }
        event.message.mentionedChannels.take(5).forEach {
            register.sender.cmdSend(translate("getid.response", event, register)
                    .apply(it.asMention, it.id), this, event)
        }
        if (event.message.mentionedUsers.isEmpty() && event.message.mentionedRoles.isEmpty()
                && event.message.mentionedChannels.isEmpty()) {
            register.sender.cmdSend(translate("getid.default",event, register)
                    .apply(event.author.id), this, event)
        }
    }
}