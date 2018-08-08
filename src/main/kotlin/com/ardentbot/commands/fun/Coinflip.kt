package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("fun")
class Coinflip : Command("coinflip", arrayOf("coin"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        register.sender.cmdSend("Flipping a coin now..", this, event, callback = {
            it.editMessage("Flipped a coin. Result: **[]**".apply(if (register.random.nextBoolean()) "Heads" else "Tails"))
                    .queueAfter(2, TimeUnit.SECONDS)
        })
    }
}