package com.ardentbot.core

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.awt.Event

class EventHooks(val register: ArdentRegister) {
    @SubscribeEvent
    fun onEvent(event: Event) {
        when (event) {
            is GuildMessageReceivedEvent -> register.process(event)
            is PrivateMessageReceivedEvent -> event.channel.sendMessage("Unfortunately, I don't support commands in private channels " +
                    "right now. Please retry in a server").queue()
        }
    }
}