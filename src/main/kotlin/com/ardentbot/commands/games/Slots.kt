package com.ardentbot.commands.games

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.selectFromList
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.*

class SlotsGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean, register: ArdentRegister)
    : Game(GameType.SLOTS, channel, creator, playerCount, isPublic, register) {
    private val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(channel.guild.getMemberById(creator))
    }

    private fun doRound(member: Member) {
        val data = member.user.getData(register)
        channel.send("How much would you like to bet? You currently have **[]** gold".apply(data.money), register)
        Sender.waitForMessage({ it.author.id == member.user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
            val bet = it.message.contentRaw.toIntOrNull()
            if (bet == null || bet <= 0 || bet > data.money) {
                channel.send("You specified an invalid bet, please retry...", register)
                doRound(member)
            } else {
                var slots = SlotsGame()
                if (!slots.won()) slots = SlotsGame() // you get two chances
                Thread.sleep(2000)
                channel.send(
                        getEmbed("Slots Results", channel).setDescription("${if (slots.won()) "Congrats, you won **[]** gold".apply(bet) else "Darn, you lost **[]** gold :(".apply(bet)}\n$slots)")
                        , register)
                if (slots.won()) data.money += bet
                else data.money -= bet
                register.database.update(data)
                rounds.add(Round(bet, slots.won(), slots.toString().replace("\n", "<br />")))
                Thread.sleep(750)
                channel.selectFromList(member, "Do you want to go again?", mutableListOf("Yes", "No"), { i, selectionMessage ->
                    if (i == 0) doRound(member)
                    else finish(member)
                    selectionMessage.delete().queue()
                }, failure = {
                    channel.send("You didn't respond in time, so I'll end the game now", register)
                    finish(member)
                }, register = register)
            }
        }, {
            channel.send("You didn't respond in time, so I'll end the game now", register)
            finish(member)
        })
    }

    private fun finish(member: Member) {
        if (rounds.size == 0) cancel(member)
        else {
            cleanup(GameDataSlots(gameId, creator, startTime!!, rounds))
        }
    }

    data class SlotsGame(val slots: ArrayList<String> = arrayListOf()) {
        init {
            val random = Random()
            for (i in 1..9) {
                slots.add(when (random.nextInt(4)) {
                    0 -> Emojis.RED_APPLE
                    1 -> Emojis.BANANA
                    2 -> Emojis.FRENCH_FRIES
                    else -> Emojis.STRAWBERRY
                }.symbol)
            }
        }

        fun won(): Boolean {
            val emoji = slots[3]
            return slots[4] == emoji && slots[5] == emoji
        }

        override fun toString(): String {
            val builder = StringBuilder()
            slots.forEachIndexed { index, s ->
                if (index % 3 == 0 && index != 0) builder.append("\n")
                builder.append("$s ")
                if (index == 5) builder.append(Emojis.LEFTWARDS_BLACK_ARROW.symbol)
            }
            return builder.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SlotsGame
            if (slots != other.slots) return false
            return true
        }

        override fun hashCode(): Int {
            return slots.hashCode()
        }
    }

    data class Round(val bet: Int, val won: Boolean, val game: String)
}

@ModuleMapping("games")
class SlotsCommand : Command("slots", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val member = event.member
        if (member.isInGameOrLobby()) event.channel.send("[], You're already in game! You can't create another game!".apply(member.asMention), register)
        /*else if (event.guild.hasGameType(GameType.SLOTS) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Slots", "<https://ardentbot.com/patreon>"))
        } */
        else {
            SlotsGame(event.channel, member.user.id, 1, false, register).startEvent()
        }
    }
}
