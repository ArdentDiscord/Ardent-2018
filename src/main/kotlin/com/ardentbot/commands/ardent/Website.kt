package com.ardentbot.commands.ardent

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("ardent")
class Website:Command("website", null,null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        event.channel.send(translate("website.response",event, register)
                .apply("https://ardentbot.com"),register)
    }
}