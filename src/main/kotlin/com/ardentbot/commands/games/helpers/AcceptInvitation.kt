package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.commands.games.isInGameOrLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class AcceptInvitation : Command("accept", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.member.isInGameOrLobby()) {
            register.sender.cmdSend("You can't join another game! You must leave the game you're currently in first", this, event)
        } else {
            gamesInLobby.forEach { game ->
                if (checkInvite(event, game, register)) return
            }
            register.sender.cmdSend("You must be invited by the creator of a game to join!", this, event)
        }
    }
}