package com.ardentbot.commands.games

import com.ardentbot.core.*
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
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
            channel.send(translate("trivia.win_info").apply(winnerUser.asMention, winner.second, questions.map { it.value }.sum())
                    + translate("trivia.prize_info") + "\n" + "**${translate("trivia.cleaning_up")}**", register)
            val data = winnerUser.getData(register)
            data.money += winner.second
            register.database.update(data)
            after(1, {
                cleanup(GameDataTrivia(gameId, creator, startTime!!, winner.first, players.without(winner.first), sc, rounds))
            })
        } else {
            if (currentRound == (roundTotal - 3)) {
                channel.send(Emojis.INFORMATION_SOURCE.cmd + translate("trivia.three_rounds_left"), register)
            }
            val question = questions[currentRound]
            channel.send(getEmbed(translate("trivia.embed_title").apply(currentRound + 1, roundTotal), channel)
                    .appendDescription("**${question.category}**\n" +
                            "${question.question}\n" +
                            "           " + translate("trivia.points").apply(question.value)), register)

            Sender.waitForMessage({ players.contains(it.author.id) && question.contains(it.message.contentRaw) },
                    { response ->
                        channel.send(translate("trivia.correct").apply(response.author.asMention, question.value), register)
                        endRound(players.without(response.author.id), question, currentRound, questions, response.author.id)
                    }, {
                channel.send(translate("trivia.no_one_correct").apply(question.answers[0]), register)
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
        val embed = getEmbed(translate("trivia.scores_embed_title").apply(currentRound + 1), channel)
        val scores = getScores()
        if (scores.second.size == 0) embed.setDescription(translate("trivia.no_one_scored"))
        else scores.first.toList().forEachIndexed { index, (u, score) ->
            embed.appendDescription("[**${index + 1}**]: **${u.toUser(register)!!.asMention}** _(${translate("trivia.points").apply(score)})_\n")
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
            register.sender.cmdSend(Emojis.INFORMATION_SOURCE.cmd + translate("trivia.generating_questions", event, register), this, event)
            val spreadsheet = sheets.spreadsheets().values().get("1qm27kGVQ4BdYjvPSlF0zM64j7nkW4HXzALFNcan4fbs", "A2:D").setKey(register.config["google"])
                    .execute()
            spreadsheet.getValues().forEach {
                if (it.getOrNull(1) != null && it.getOrNull(2) != null) {
                    questions.add(TriviaQuestion(it[1] as String, (it[2] as String).split("~"), it[0] as String,
                            (it.getOrNull(3) as String?)?.toIntOrNull() ?: 125))
                }
            }
            register.sender.cmdSend(Emojis.WHITE_HEAVY_CHECKMARK.cmd + translate("trivia.generated_questions", event, register), this, event)
        }
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("solo", event.guild, register) == true -> {
                if (event.member!!.isInGameOrLobby()) event.channel.send(translate("games.already_in_game", event, register).apply(event.member!!.asMention), register)
                /*else if (event.guild.hasGameType(GameType.TRIVIA) && !event.member!!.hasDonationLevel(event.textChannel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
                    event.channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Trivia", "<https://ardentbot.com/patreon>"))
                } */
                else TriviaGame(event.channel, event.member!!.user.id, 1, false, register).startEvent()
            }
            arg?.isTranslatedArgument("multi", event.guild, register) == true -> {
                if (event.member!!.isInGameOrLobby()) event.channel.send(translate("games.already_in_game", event, register).apply(event.member!!.asMention), register)
                /*else if (event.guild.hasGameType(GameType.TRIVIA) && !event.member!!.hasDonationLevel(event.textChannel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
                    event.channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Trivia", "<https://ardentbot.com/patreon>"))
                } */
                else {
                    event.channel.selectFromList(event.member!!, translate("trivia.make_public", event, register),
                            mutableListOf(translate("yes", event, register), translate("no", event, register)), { public, _ ->
                        val isPublic = public == 0
                        event.channel.send(translate("trivia.how_many_players", event, register), register)
                        Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { playerCount ->
                            val count = playerCount.message.contentRaw.toIntOrNull() ?: 999
                            if (count == 0) event.channel.send(translate("trivia.invalid_number_cancel", event, register), register)
                            else {
                                val game = TriviaGame(event.channel, event.author.id, count, isPublic, register)
                                gamesInLobby.add(game)
                            }
                        }, {
                            event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    translate("sender.timeout", event, register).apply(20), register)
                        })
                    }, register = register)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val solo = Argument("solo")
    val multi = Argument("multi")
}

data class SanitizedTriviaRound(val hasWinner: Boolean, val winner: User?, val losers: List<User?>, val question: TriviaQuestion)
data class SanitizedTrivia(val creator: User, val id: Any, val winner: User, val losers: List<User>, val scores: List<Pair<String, Int>>,
                           val rounds: List<SanitizedTriviaRound>)