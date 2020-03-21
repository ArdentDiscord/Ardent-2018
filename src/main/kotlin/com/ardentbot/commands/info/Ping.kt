package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.commands.Command
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class Ping : Command("ping", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val currentTime = System.currentTimeMillis()
        register.sender.cmdSend("*${translate("ping.wow",event, register)}*", this, event,
                callback = {
                    val latency = System.currentTimeMillis() - currentTime
                    it.editMessage(translate("ping.response", event, register).apply(latency, if (latency < 150) "Normal :)" else "Slow :(")).queue()
                })
    }
}