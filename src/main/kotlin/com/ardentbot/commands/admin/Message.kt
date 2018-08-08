package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.commands.Precondition
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import com.ardentbot.kotlin.removeIndices
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("ardent")
class Message : Command("message", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.size < 3) {
            register.sender.cmdSend("/message name discrim message", this, event)
            return
        }
        val name = arguments[0]
        val discrim = arguments[1]
        register.jda.getUsersByName(name, true).forEach {
            if (discrim == it.discriminator) it.openPrivateChannel()
                    .queue {
                        it.sendMessage(Emojis.WAVING_HANDS.cmd +
                                "Hey, you got a message from the Ardent developers: []"
                                        .apply(arguments.toMutableList().removeIndices(0, 1).concat()) +
                                "\n----------\n" +
                                "Did you know Ardent has a community server where you can suggest new features and get help? " +
                                "https://discord.gg/MANYqyq")
                                .queue {
                                    register.sender.cmdSend("Successfully sent message", this, event)
                                }
                    }
        }
    }

    val developer = Precondition({ it.event.author.id == "169904324980244480" }, {
        listOf(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to be a bot developer to use this command!")
    })
}