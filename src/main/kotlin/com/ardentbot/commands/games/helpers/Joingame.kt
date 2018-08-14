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
                register.sender.cmdSend(translate("join.id", event, register), this, event)
                return
            }
            gamesInLobby.forEach { game ->
                if (game.channel.guild == event.guild) {
                    if (event.member.isInGameOrLobby()) {
                        register.sender.cmdSend(translate("accept.ingame", event, register), this, event)
                    } else {
                        if (game.isPublic || checkInvite(event, game, register)) {
                            game.players.add(event.author.id)
                            register.sender.cmdSend(translate("games.joined", event, register).apply(event.author.display(),
                                    game.creator.toUser(register)?.display()
                                            ?: translate("unknown", event, register), game.type.readable) + "\n" +
                                    translate("games.current_lobby", event, register).apply(game.players.toUsersDisplay(register)), this, event)
                        }
                    }
                    return
                }
            }
            register.sender.cmdSend(translate("join.no_game", event, register).apply(id), this, event)
        } else register.sender.cmdSend(translate("join.id", event, register), this, event)

    }
}