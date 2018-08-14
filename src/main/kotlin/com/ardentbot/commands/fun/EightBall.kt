package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.concat
import com.ardentbot.kotlin.encode
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("fun")
class EightBall : Command("8ball", arrayOf("8b"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) register.sender.cmdSend("${Emojis.HEAVY_MULTIPLICATION_X} " + translate("8ball.no",event, register),
                this, event)
        else register.sender.cmdSend(":8ball: " + register.database.deserializeWebsite("https://8ball.delegator.com/magic/JSON/${arguments.concat().encode()}",
                BallResponse::class.java).magic.answer, this, event)
    }
}

data class BallResponse(val magic: Magic)
data class Magic(val question: String, val answer: String, val type: String)