package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.toUsersDisplay
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Cancel : Command("cancel", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        var found = false
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id) {
                found = true
                register.sender.cmdSend(Emojis.HEAVY_EXCLAMATION_MARK_SYMBOL.cmd +
                        translate("cancel.confirmation", event, register).apply(game.type.readable) + "\n" +
                        translate("games.current_lobby", event, register).apply(game.players.toUsersDisplay(register)), this, event)
                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id },
                        {
                            if (it.message.contentRaw.startsWith("y", true) || it.message.contentRaw.isTranslatedPhrase("yes", event.guild, register)) {
                                game.cancel(event.member)
                            } else register.sender.cmdSend("${Emojis.BALLOT_BOX_WITH_CHECK} " + translate("cancel.keep_in_lobby", event, register), this, event)
                        })
            }
        }
        if (!found) register.sender.cmdSend(Emojis.NO_ENTRY_SIGN.cmd + translate("games.not_creator_in_lobby", event, register),
                this, event)
    }
}