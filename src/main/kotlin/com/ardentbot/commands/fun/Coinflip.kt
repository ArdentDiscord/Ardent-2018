package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("fun")
class Coinflip : Command("coinflip", arrayOf("coin"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        register.sender.cmdSend(translate("coinflip.flipping", event, register), this, event, callback = {
            it.editMessage(translate("coinflip.result", event, register).apply(if (register.random.nextBoolean())
                translate("coinflip.heads", event, register) else translate("coinflip.tails", event, register)))
                    .queueAfter(2, TimeUnit.SECONDS)
        })
    }
}