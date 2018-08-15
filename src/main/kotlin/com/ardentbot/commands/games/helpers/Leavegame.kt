package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Leavegame : Command("leavegame", arrayOf("leaveg"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                register.sender.cmdSend(translate("games.cant_leave_own", event, register), this, event)
                return
            } else if (game.players.contains(event.author.id)) {
                game.players.remove(event.author.id)
                register.sender.cmdSend(translate("leavegame.response", event, register).apply(event.author.asMention, game.creator.toUser(register)?.display()
                        ?: translate("unknown", event, register)), this, event)
                return
            }
        }
        register.sender.cmdSend(translate("games.not_in_lobby", event, register), this, event)

    }
}