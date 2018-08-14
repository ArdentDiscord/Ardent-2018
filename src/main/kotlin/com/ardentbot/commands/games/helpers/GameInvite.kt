package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.Game
import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.commands.games.invites
import com.ardentbot.commands.games.isInGameOrLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit
import kotlin.collections.set

@ModuleMapping("games")
class GameInvite : Command("gameinvite", arrayOf("ginvite", "gi"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.isPublic) {
                    register.sender.cmdSend(translate("gameinvite.public", event, register), this, event)
                    return
                }
                getUser(arguments.concat(), event, this, register) { user ->
                    if (user == null) register.sender.cmdSend(translate("general.specify_or_mention_user", event, register), this, event)
                    else {
                        val toInvite = user
                        when {
                            invites.containsKey(toInvite.id) -> register.sender.cmdSend(translate("gameinvite.has_invite", event, register), this, event)
                            toInvite.isInGameOrLobby() -> register.sender.cmdSend(translate("gameinvite.already_in", event, register), this, event)
                            else -> {
                                invites[toInvite.id] = game
                                register.sender.cmdSend(translate("gameinvite.response", event, register)
                                        .apply(toInvite.name, event.member.asMention, game.type.readable), this, event)
                                val delay = 45
                                Sender.scheduledExecutor.schedule({
                                    if (invites.containsKey(toInvite.id)) {
                                        register.sender.cmdSend(translate("gameinvite.expired", event, register)
                                                .apply(toInvite.asMention, game.creator.toUser(register)?.display()
                                                        ?: "unknown", delay), this, event)
                                        invites.remove(toInvite.id)
                                    }
                                }, delay.toLong(), TimeUnit.SECONDS)
                            }
                        }
                    }
                }
                return
            }
        }
        register.sender.cmdSend(Emojis.NO_ENTRY_SIGN.cmd + translate("games.not_creator_in_lobby", event, register), this, event)
    }
}

fun checkInvite(event: GuildMessageReceivedEvent, game: Game, register: ArdentRegister): Boolean {
    return if (!game.started && gamesInLobby.contains(game) && invites.containsKey(event.author.id) && invites[event.author.id]!!.gameId == game.gameId) {
        invites.remove(event.author.id)
        game.players.add(event.author.id)
        val language = register.database.getGuildData(event.guild).language ?: Language.ENGLISH
        register.sender.send(register.translationManager.translate("games.joined", language).apply(event.author.display(),
                game.creator.toUser(register)?.display() ?: "unknown", game.type.readable) + "\n" +
                register.translationManager.translate("games.current_lobby", language)
                        .apply(game.players.toUsersDisplay(register)),
                null, event.channel, register.selfUser, null)

        true
    } else false
}