package com.ardentbot.commands.ardent

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("ardent")
class Suggest : Command("suggest", null, 300) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.size < 10) {
            register.sender.cmdSend(translate("suggest.more_arguments", event, register), this, event)
            users.remove(event.author.id)
        } else {
            register.getTextChannel("351370720707608586")!!
                    .sendMessage("**Suggestion from ${event.author.display()}**: ${arguments.joinToString(" ")}").queue()
            register.sender.cmdSend(translate("suggest.response", event, register).apply("https://discord.gg/Dtg23A7"),
                    this, event)
        }
    }
}