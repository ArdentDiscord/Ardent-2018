package com.ardentbot.commands.games

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.*
import java.util.concurrent.TimeUnit


class BlackjackGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean, register: ArdentRegister)
    : Game(GameType.BLACKJACK, channel, creator, playerCount, isPublic, register) {
    private val roundResults = mutableListOf<Round>()
    override fun onStart() {
        val user = players.map { it.toUser(register)!! }.toList()[0]
        doRound(user)
    }

    fun doRound(user: User) {
        val playerData = user.getData(register)
        if (playerData.money == 0L) {
            playerData.money += 15
            register.database.update(playerData)
            channel.send("Because you were broke, the Blackjack Gods took pity on you and gave you **15.0** gold to bet with", register)
        }
        channel.send("How much would you like to bet, []? You current have a balance of **[]** gold".apply(user.asMention, playerData.money), register)
        Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
            val bet = it.message.contentRaw.toLongOrNull()
            if (bet == null || bet <= 0 || bet > playerData.money) {
                channel.send("You specified an invalid amount.. resetting the round", register)
                doRound(user)
            } else {
                val dealerHand = Hand(true).blackjackPlus(2)
                val userHand = Hand().blackjackPlus(1)
                display(dealerHand, userHand, "You've been dealt 1 card." + " " +
                        "The dealer's second card is hidden." + " " +
                        "The goal is to get as close as possible to **21**. Type `" +
                        "hit" + "` if you'd like to get another card or `" + "stay" +
                        "` to stay at your current amount", post = {
                    wait(bet, dealerHand, userHand, user, it)
                })
            }

        }, { cancel(user) })
    }

    fun wait(bet: Long, dealerHand: Hand, userHand: Hand, user: User, previous: Message?) {
        Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
            previous?.delete()?.queue()
            when (it.message.contentRaw) {
                "hit" -> {
                    it.message.delete().queue()
                    userHand.blackjackPlus(1)
                    if (userHand.value() >= 21) displayRoundScore(bet, dealerHand, userHand, user)
                    else {
                        display(dealerHand, userHand, "You've been dealt 1 card." + " " +
                                "The dealer's second card is hidden." + " " +
                                "The goal is to get as close as possible to **21**. Type `" +
                                "hit" + "` if you'd like to get another card or `" + "stay" +
                                "` to stay at your current amount", post = {
                            wait(bet, dealerHand, userHand, user, it)
                        })
                    }
                }
                "stay" -> {
                    it.message.delete().queue()
                    channel.send("Generating dealer cards...", register)
                    Thread.sleep(1500)
                    while (dealerHand.value() < 17) dealerHand.blackjackPlus(1)
                    displayRoundScore(bet, dealerHand, userHand, user)
                }
                else -> {
                    register.sender.send("You specified an invalid response - please retry", null, channel, register.selfUser, null, callback = {
                        wait(bet, dealerHand, userHand, user, it)
                    })
                    return@waitForMessage
                }
            }
        }, {
            channel.send("[], you didn't specify a response and thus lost!".apply(user.asMention), register)
            cancel(user, complain = false)
        }, 15, TimeUnit.SECONDS)
    }

    fun display(dealerHand: Hand, userHand: Hand, message: String, end: Boolean = false, post: (Message) -> Unit) {
        val embed = getEmbed("Blackjack | Hand Values", channel)
                .setDescription(message)
                .addField("Your Hand", "$userHand (${userHand.value()})", true)
                .addBlankField(true)
        if (dealerHand.cards.size == 2 && !end) embed.addField("Dealer's Hand", "$dealerHand (${dealerHand.cards[0].value.representation} + ?)", true)
        else embed.addField("Dealer's Hand", "$dealerHand (${dealerHand.value()})", true)
        register.sender.send(embed, null, channel, register.selfUser, null, callback = {
            post(it)
        })
    }

    fun displayRoundScore(bet: Long, dealerHand: Hand, userHand: Hand, user: User) {
        val result = when {
            userHand.value() > 21 -> Result.LOST
            dealerHand.value() > 21 -> Result.WON
            userHand.value() == dealerHand.value() -> Result.TIED
            userHand.value() > dealerHand.value() -> Result.WON
            else -> Result.LOST
        }
        val playerData = user.getData(register)
        val message = when (result) {
            Result.LOST -> {
                playerData.money -= bet
                register.database.update(playerData)
                "**Sorry, you lost [] gold!**".apply(bet)
            }
            Result.WON -> {
                playerData.money += bet
                register.database.update(playerData)
                "**Congratulations, you won [] gold!**".apply(bet)
            }
            Result.TIED -> "**You tied and didn't lose the [] you bet!**".apply(bet)
        }
        roundResults.add(Round(result, userHand.end(), dealerHand.end(), bet))
        display(dealerHand, userHand, message, true, post = {
            channel.sendMessage("Would you like to go again? Type `" + "yes" + "` to replay or `" + "no" +
                    "` to end the game").queueAfter(2, TimeUnit.SECONDS) { response ->
                Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
                    response.delete().queue()
                    when (it.message.contentRaw) {
                        "yes" -> doRound(user)
                        else -> {
                            val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
                            cleanup(gameData)
                        }
                    }
                }, {
                    val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
                    cleanup(gameData)
                })
            }
        })

    }

    enum class Result {
        WON, LOST, TIED;

        override fun toString(): String {
            return when (this) {
                BlackjackGame.Result.WON -> "<font color=\"green\">Won</font>"
                BlackjackGame.Result.LOST -> "<font color=\"red\">Lost</font>"
                BlackjackGame.Result.TIED -> "<font color=\"orange\">Tied</font>"
            }
        }
    }

    class Round(val won: Result, val userHand: Hand, val dealerHand: Hand, val bet: Long)
    class Hand(val dealer: Boolean = false, val cards: MutableList<Card> = mutableListOf(), var end: Boolean = false) {
        val random = Random()
        fun blackjackPlus(cardAmount: Int): Hand {
            (1..cardAmount).forEach { _ ->
                cards.add(generate())
                if (value() > 21) cards.forEach { if (it.value == BlackjackValue.ACE) it.value.representation = 1 }
            }
            return this
        }

        fun end(): Hand {
            end = true
            return this
        }

        fun generate(): Card {
            val card = Card(Suit.values()[random.nextInt(4)], BlackjackValue.values()[random.nextInt(13)])
            return if (cards.contains(card)) generate()
            else card
        }

        override fun toString(): String {
            return if (cards.size == 2 && dealer && !end) "${cards[0]}, ?"
            else cards.joinToString { it.toString() }
        }

        fun value(): Int {
            var value = 0
            cards.forEach { value += it.value.representation }
            return value
        }
    }

    data class Card(val suit: Suit, val value: BlackjackValue) {

        override fun toString(): String {
            return "$value$suit"
        }
    }

    enum class Suit {
        HEART, SPADE, CLUB, DIAMOND;

        override fun toString(): String {
            return when (this) {
                HEART -> "♥"
                SPADE -> "♠"
                CLUB -> "♣"
                DIAMOND -> "♦"
            }
        }
    }

    enum class BlackjackValue(var representation: Int) {
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        JACK(10),
        QUEEN(10),
        KING(10),
        ACE(11);

        override fun toString(): String {
            return when (this) {
                ACE -> "A"
                KING -> "K"
                QUEEN -> "Q"
                JACK -> "J"
                else -> representation.toString()
            }
        }
    }
}

@ModuleMapping("games")
class BlackjackCommand : Command("blackjack", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val member = event.member
        if (member.isInGameOrLobby()) event.channel.send("[], You're already in game! You can't create another game!".apply(member.asMention), register)

        /*else if (event.guild.hasGameType(GameType.BLACKJACK) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Blackjack", "<https://ardentbot.com/patreon>"))
        } */
        else {
            BlackjackGame(event.channel, member.user.id, 1, false, register).startEvent()
        }

    }
}