package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.GameType
import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Forcestart : Command("start", arrayOf("forcestart"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.players.size == 1 && game.type != GameType.TRIVIA) {
                    register.sender.cmdSend(translate("start.only_one", event, register), this, event)
                } else game.startEvent()
                return
            }
        }
        register.sender.cmdSend(Emojis.NO_ENTRY_SIGN.cmd + translate("games.not_creator_in_lobby", event, register), this, event)
    }
}