package com.ardentbot.commands.games

import com.ardentbot.core.*
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.*

val questions = mutableListOf<TriviaQuestion>()

class TriviaGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean, register: ArdentRegister)
    : Game(GameType.TRIVIA, channel, creator, playerCount, isPublic, register) {
    val ardent = channel.guild.selfMember!!
    private val rounds = mutableListOf<Round>()
    private val roundTotal = 9
    private val questions = getTriviaQuestions(roundTotal)

    override fun onStart() {
        doRound(0, questions)
    }

    private fun doRound(currentRound: Int, questions: List<TriviaQuestion>) {
        if (currentRound == roundTotal) {
            val sc = getScores().first.toList().sortedByDescending { it.second }.toMap()
            val winner = sc.toList()[0]
            val winnerUser = winner.first.toUser(register)!!
            channel.send("Congrats to [] for winning with **[]** of **[]** points possible!"
                    .apply(winnerUser.asMention, winner.second, questions.map { it.value }.sum())
                    + "They'll receive that amount in gold as a prize!"
                    + "\n" + "**Cleaning game up..**", register)
            val data = winnerUser.getData(register)
            data.money += winner.second
            register.database.update(data)
            after(1, {
                cleanup(GameDataTrivia(gameId, creator, startTime!!, winner.first, players.without(winner.first), sc, rounds))
            })
        } else {
            if (currentRound == (roundTotal - 3)) {
                channel.send("${Emojis.INFORMATION_SOURCE} " + "There are only **3** rounds left!", register)
            }
            val question = questions[currentRound]
            channel.send(getEmbed("Trivia | Question [] of []".apply(currentRound + 1, roundTotal), channel)
                    .appendDescription("**${question.category}**\n" +
                            "${question.question}\n" +
                            "           " + "**[]** points".apply(question.value)), register)

            Sender.waitForMessage({ players.contains(it.author.id) && question.contains(it.message.contentRaw) },
                    { response ->
                        channel.send("[] guessed the correct answer and got **[]** points!".apply(response.author.asMention, question.value), register)
                        endRound(players.without(response.author.id), question, currentRound, questions, response.author.id)
                    }, {
                channel.send("No one got it right! The correct answer was **[]**".apply(question.answers[0]), register)
                endRound(players, question, currentRound, questions)
            }, time = 20)
        }
    }

    private fun endRound(losers: MutableList<String>, question: TriviaQuestion, currentRound: Int, questions: List<TriviaQuestion>, vararg winner: String) {
        rounds.add(Round(winner, losers, question))
        if ((currentRound + 1) % 3 == 0) showScores(currentRound)
        after(2, { doRound(currentRound + 1, questions) })
    }

    private fun showScores(currentRound: Int) {
        val embed = getEmbed("Trivia Scores | Round []".apply(currentRound + 1), channel)
        val scores = getScores()
        if (scores.second.size == 0) embed.setDescription("No one has scored yet!")
        else scores.first.toList().forEachIndexed { index, (u, score) ->
            embed.appendDescription("[**${index + 1}**]: **${u.toUser(register)!!.asMention}** *($score points)*\n")
        }
        channel.send(embed, register)
    }


    private fun getScores(): Pair<MutableMap<String, Int /* Point values */>, HashMap<String, Int /* Amt of Qs correct */>> {
        val points = hashMapOf<String, Int>()
        val questions = hashMapOf<String, Int>()
        rounds.forEach { (winners, _, q) ->
            if (winners.isNotEmpty()) {
                winners.forEach { winner ->
                    if (points.containsKey(winner)) points.replace(winner, points[winner]!! + q.value)
                    else points[winner] = q.value
                    questions.increment(winner)
                }
            }
        }
        players.forEach { points.putIfAbsent(it, 0) }
        return Pair(points.toList().sortedByDescending { it.second }.toMap().toMutableMap(), questions)
    }

    data class Round(val winners: Array<out String>, val losers: MutableList<String>, val question: TriviaQuestion) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Round
            if (!Arrays.equals(winners, other.winners)) return false
            if (losers != other.losers) return false
            if (question != other.question) return false
            return true
        }

        override fun hashCode(): Int {
            var result = winners.let { Arrays.hashCode(it) }
            result = 31 * result + losers.hashCode()
            result = 31 * result + question.hashCode()
            return result
        }
    }
}

data class TriviaQuestion(val question: String, val answers: List<String>, val category: String, val value: Int) {
    fun contains(possible: String): Boolean {
        return answers.map { clean(it) }.contains(clean(possible))
    }

    private fun clean(string: String): String {
        return string.remove("the").remove("a").remove("an").remove(".").remove(",")
                .toLowerCase()
    }
}

fun getTriviaQuestions(number: Int): List<TriviaQuestion> {
    val list = mutableListOf<TriviaQuestion>()
    val random = Random()
    (1..number).forEach {
        val q = questions[random.nextInt(questions.size)]
        if (!list.contains(q)) list.add(q)
    }
    return list
}


@ModuleMapping("games")
class TriviaCommand : Command("trivia", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (questions.isEmpty()) {
            register.sender.cmdSend(Emojis.INFORMATION_SOURCE.cmd + "I'm generating questions now.. (this shouldn't happen for a while)", this, event)
            val spreadsheet = sheets.spreadsheets().values().get("1qm27kGVQ4BdYjvPSlF0zM64j7nkW4HXzALFNcan4fbs", "A2:D").setKey(register.config["google"])
                    .execute()
            spreadsheet.getValues().forEach {
                if (it.getOrNull(1) != null && it.getOrNull(2) != null) {
                    questions.add(TriviaQuestion(it[1] as String, (it[2] as String).split("~"), it[0] as String,
                            (it.getOrNull(3) as String?)?.toIntOrNull() ?: 125))
                }
            }
            register.sender.cmdSend(Emojis.WHITE_HEAVY_CHECKMARK.cmd + "Generated questions", this, event)
        }
        when (arguments.getOrNull(0)) {
            "solo" -> {
                if (event.member.isInGameOrLobby()) event.channel.send("[], You're already in game! You can't create another game!"
                        .apply(event.member.asMention), register)
                /*else if (event.guild.hasGameType(GameType.TRIVIA) && !event.member.hasDonationLevel(event.textChannel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
                    event.channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Trivia", "<https://ardentbot.com/patreon>"))
                } */
                else TriviaGame(event.channel, event.member.user.id, 1, false, register).startEvent()
            }
            "multi", "multiplayer" -> {
                if (event.member.isInGameOrLobby()) event.channel.send("[], You're already in game! You can't create another game!"
                        .apply(event.member.asMention), register)
                /*else if (event.guild.hasGameType(GameType.TRIVIA) && !event.member.hasDonationLevel(event.textChannel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
                    event.channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Trivia", "<https://ardentbot.com/patreon>"))
                } */
                else {
                    event.channel.selectFromList(event.member, "Would you like this game to be open to everyone to join?", mutableListOf("Yes", "No"), { public, _ ->
                        val isPublic = public == 0
                        event.channel.send("How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)", register)
                        Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { playerCount ->
                            val count = playerCount.message.contentRaw.toIntOrNull() ?: 999
                            if (count == 0) event.channel.send("Invalid number provided, cancelling setup", register)
                            else {
                                val game = TriviaGame(event.channel, event.author.id, count, isPublic, register)
                                gamesInLobby.add(game)
                            }
                        }, {
                            event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Canceling setup.. you didn't respond in time! (**[]** seconds)"
                                    .apply(20), register)
                        })
                    }, register = register)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val solo = ArgumentInformation("solo", "start a solo trivia game")
    val multi = ArgumentInformation("multi/multiplayer", "start a multiplayer trivia game")
}

data class SanitizedTriviaRound(val hasWinner: Boolean, val winner: User?, val losers: List<User?>, val question: TriviaQuestion)
data class SanitizedTrivia(val creator: User, val id: Any, val winner: User, val losers: List<User>, val scores: List<Pair<String, Int>>,
                           val rounds: List<SanitizedTriviaRound>)