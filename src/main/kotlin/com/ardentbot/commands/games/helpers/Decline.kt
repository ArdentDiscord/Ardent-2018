package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.invites
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Decline : Command("decline", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (invites.containsKey(event.author.id)) {
            val game = invites[event.author.id]!!
            register.sender.cmdSend("[] declined an invite to **[]**'s game of **[]**".apply(event.author.asMention,
                    game.creator.toUser(register)?.display() ?: "unknown", game.type.readable), this, event)
            invites.remove(event.author.id)
        } else register.sender.cmdSend("You don't have a pending invite to decline!", this, event)
    }
}