package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.commands.games.isInGameOrLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.toUsersDisplay
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Joingame : Command("join", arrayOf("joingame"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.size == 1) {
            val id = arguments[0].replace("#", "").toIntOrNull()
            if (id == null) {
                register.sender.cmdSend("You need to include a Game ID! Example: **/join #123456**", this, event)
                return
            }
            gamesInLobby.forEach { game ->
                if (game.channel.guild == event.guild) {
                    if (event.member.isInGameOrLobby()) {
                        register.sender.cmdSend("You can't join another game! You must leave the game you're currently in first",
                                this, event)
                    } else {
                        if (game.isPublic || checkInvite(event, game, register)) {
                            game.players.add(event.author.id)
                            register.sender.cmdSend("**[]** has joined **[]**'s game of []".apply(event.author.display(),
                                    game.creator.toUser(register)?.display() ?: "unknown", game.type.readable) + "\n" +
                                    "Current players in lobby: *[]*".apply(game.players.toUsersDisplay(register)), this, event)
                        }
                    }
                    return
                }
            }
            register.sender.cmdSend("There's not a game in lobby with the ID of **#[]**".apply(id), this, event)
        } else register.sender.cmdSend("You need to include a Game ID! Example: **/join #123456**", this, event)

    }
}