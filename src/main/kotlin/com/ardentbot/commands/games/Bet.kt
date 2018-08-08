package com.ardentbot.commands.games

import com.ardentbot.core.*
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.format
import com.ardentbot.kotlin.remove
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class BetGame(channel: TextChannel, creator: String, register: ArdentRegister) : Game(GameType.BETTING, channel, creator, 1, false, register) {
    private val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(creator.toUser(register)!!)
    }

    private fun doRound(user: User) {
        val data = user.getData(register)
        channel.send("How much would you like to bet? You current have **[] gold**. Type the amount below. You can also bet a **percentage** of your net worth, e.g. *40%*".apply(data.money), register)
        Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
            val content = it.message.contentRaw.removePrefix("/bet ").remove("bet ")
            if (content.equals("cancel", true)) {
                channel.send("Cancelling game...", register)
                cancel(user)
            } else {
                val bet = if (content.contains("%")) content.removeSuffix("%").toDoubleOrNull()?.div(100)?.times(data.money)?.toInt() else content.toIntOrNull()
                if (bet != null) {
                    if (bet > data.money || bet <= 0) {
                        channel.send("You specified an invalid bet amount! Please retry or type `cancel` to cancel the game", register)
                        doRound(user)
                        return@waitForMessage
                    } else {
                        channel.selectFromList(channel.guild.getMember(user), "What color will the next card I draw be?", mutableListOf("Black", "Red"), { selection, _ ->
                            val suit = BlackjackGame.Hand(false, end = false).generate().suit
                            val won = when (suit) {
                                BlackjackGame.Suit.HEART, BlackjackGame.Suit.DIAMOND -> selection == 1
                                else -> selection == 0
                            }
                            if (won) {
                                data.money += bet
                                channel.send("Congrats, you won - the suit was []! I've added **[] gold** to your profile - new balance: **[] gold**".apply(suit, bet, data.money.format()), register)
                            } else {
                                data.money -= bet
                                channel.send("Sorry, you lost - the suit was [] :( I've removed **[] gold** from your profile - new balance: **[] gold**".apply(suit, bet, data.money.format()), register)
                            }
                            register.database.update(data)
                            rounds.add(Round(won, bet.toDouble(), suit))
                            channel.send("Would you like to go again? Type `yes` if so or `no` to end the game", register)

                            Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
                                if (it.message.contentRaw.startsWith("y", true)) doRound(user)
                                else {
                                    channel.send("Ending the game and inserting data into the database..", register)
                                    val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                    cleanup(gameData)
                                }
                            }, {
                                channel.send("Ending the game and inserting data into the database..", register)
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            })

                        }, failure = {
                            if (rounds.size == 0) {
                                channel.send("Invalid response... Cancelling game now", register)
                                cancel(user)
                            } else {
                                channel.send("Invalid response... ending the game and inserting data into the database..", register)
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            }
                        }, register = register)
                    }
                } else {
                    if (rounds.size == 0) {
                        channel.send("Invalid bet amount... Cancelling game now", register)
                        cancel(user)
                    } else {
                        channel.send("Invalid bet amount... ending the game and inserting data into the database..", register)
                        val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                        cleanup(gameData)
                    }
                }
            }
        }, { cancel(user) }, time = 20)
    }

    data class Round(val won: Boolean, val betAmount: Double, val suit: BlackjackGame.Suit)
}

@ModuleMapping("games")
class BetCommand : Command("bet", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val member = event.member
        if (member.isInGameOrLobby()) event.channel.send("[], you're already in game! You can't create another game!".apply(member.asMention), register)
        else BetGame(event.channel, member.user.id, register).startEvent()
    }
}