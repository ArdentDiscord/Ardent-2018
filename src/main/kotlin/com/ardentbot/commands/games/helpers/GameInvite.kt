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
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.toUsersDisplay
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("games")
class GameInvite : Command("gameinvite", arrayOf("ginvite", "gi"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.isPublic) {
                    register.sender.cmdSend("You don't need to invite people to a public game! Everyone can join", this, event)
                    return
                }
                val mentionedUsers = event.message.mentionedUsers
                if (mentionedUsers.size == 0 || mentionedUsers[0].isBot) register.sender.cmdSend("You need to mention at least one member to invite them", this, event)
                else {
                    mentionedUsers.forEach { toInvite ->
                        when {
                            invites.containsKey(toInvite.id) -> register.sender.cmdSend("You can't invite a member who already has a pending invite!", this, event)
                            toInvite.isInGameOrLobby() -> register.sender.cmdSend("This person is already in a lobby or ingame!", this, event)
                            else -> {
                                invites[toInvite.id] = game
                                register.sender.cmdSend("[], you're being invited by [] to join a game of **[]**! Type */accept* to accept this invite and join the game or decline by typing */decline*"
                                        .apply(toInvite.name, event.member.asMention, game.type.readable), this, event)
                                val delay = 45
                                Sender.scheduledExecutor.schedule({
                                    if (invites.containsKey(toInvite.id)) {
                                        register.sender.cmdSend("[], your invite to **[]**'s game has expired after [] seconds."
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
        register.sender.cmdSend(Emojis.NO_ENTRY_SIGN.cmd + "You're not the creator of a game in lobby!", this, event)
    }
}

fun checkInvite(event: GuildMessageReceivedEvent, game: Game, register: ArdentRegister): Boolean {
    return if (!game.started && gamesInLobby.contains(game) && invites.containsKey(event.author.id) && invites[event.author.id]!!.gameId == game.gameId) {
        invites.remove(event.author.id)
        game.players.add(event.author.id)
        register.sender.send("**[]** has joined **[]**'s game of []".apply(event.author.display(),
                game.creator.toUser(register)?.display() ?: "unknown", game.type.readable) + "\n" +
                "Current players in lobby: *[]*".apply(game.players.toUsersDisplay(register)),
                null, event.channel, register.selfUser, null)

        true
    } else false
}