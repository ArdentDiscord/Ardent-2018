package com.ardentbot.commands.games

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Sender
import com.ardentbot.core.database.DbObject
import com.ardentbot.core.database.getLanguage
import com.ardentbot.core.database.getUserData
import com.ardentbot.core.toUser
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import org.apache.commons.lang3.exception.ExceptionUtils
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val invites = ConcurrentHashMap<String, Game>()

val gamesInLobby = ConcurrentLinkedQueue<Game>()
val activeGames = ConcurrentLinkedQueue<Game>()

/**
 * Abstracted Game features, providing standardized methods for cleanup, startup, and lobbies.
 * @param creator Discord user ID of the user creating the game
 * @param isPublic Should this game be treated as public? (will prompt lobby setup)
 */
abstract class Game(val type: GameType, val channel: TextChannel, val creator: String, val playerCount: Int, var isPublic: Boolean,
                    val register: ArdentRegister) {
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    var gameId: Long = 0
    val players = mutableListOf<String>()
    private val creation: Long
    var startTime: Long? = null
    var started = false

    init {
        gameId = type.findNextId(register)
        players.add(creator)
        this.announceCreation()
        creation = System.currentTimeMillis()
        if (isPublic) {
            displayLobby()
            scheduledExecutor.scheduleAtFixedRate({ displayLobby() }, 60, 47, TimeUnit.SECONDS)
        } else if (type != GameType.BLACKJACK && type != GameType.BETTING && playerCount > 1) {
            register.sender.send("Created your game! [], use **/gameinvite @User** to invite someone to your game".apply(creator.toUser(register)?.asMention
                    ?: "unable to determine creator"), null, channel, register.selfUser, null)
        }
        scheduledExecutor.scheduleWithFixedDelay({
            if (playerCount == players.size) {
                channel.sendMessage("Starting a game of type **[]** with **[]** players ([])"
                        .apply(type.readable, players.size, players.toUsersDisplay(register))
                ).queueAfter(2, TimeUnit.SECONDS) { _ -> startEvent() }
                scheduledExecutor.shutdown()
            }
        }, 1, 1, TimeUnit.SECONDS)
        scheduledExecutor.schedule({
            if (gamesInLobby.contains(this)) {
                register.sender.send("**10** minutes have passed in lobby, so I cancelled the game setup.",
                        null, channel, register.selfUser, null)
                cancel(creator.toUser(register)!!, false)
            }
        }, 10, TimeUnit.MINUTES)
    }

    /**
     * Displays the lobby prompt for this game and returns the sent [Message] if not null
     */
    private fun displayLobby(): Message? {
        val member = channel.guild.selfMember

        val embed = getEmbed("${type.readable} Game Lobby", creator.toUser(register)!!, channel.guild, Color.ORANGE)
                .setFooter("Ardent Game Engine - by []".apply("Adam#9261"), member.user.avatarUrl)
                .setDescription("This lobby has been active for []".apply(((System.currentTimeMillis() - creation) / 1000).toMinutesAndSeconds()) + "\n" +
                        "It currently has **[]** of **[]** players required to start | []".apply(players.size, playerCount, players.toUsersDisplay(register)) + "\n" +
                        "To start, the host can also type */forcestart*" + "\n\n" +
                        "This game was created by __[]__".apply(creator.toUser(register)?.display()
                                ?: "unable to determine"))
        var me: Message? = null
        channel.sendMessage(embed.build()).queue { m ->
            register.sender.send("Join by typing **/join #$gameId**\n" +
                    "*You can cancel this game by typing /cancel*", null, channel, register.selfUser, null, callback = {
                it.delete().queueAfter(90, TimeUnit.SECONDS)
                m.delete().queueAfter(90, TimeUnit.SECONDS)
            })
            me = m
        }
        return me
    }

    /**
     * Commence game startup, this must be called. Removes pending invites & changes game state
     */
    fun startEvent() {
        if (!started) {
            invites.forEach { i, g -> if (g.gameId == gameId) invites.remove(i) }
            scheduledExecutor.shutdownNow()
            gamesInLobby.remove(this)
            activeGames.add(this)
            startTime = System.currentTimeMillis()
            channel.send("Let's play **[]**!".apply(type.readable), register)
            Sender.scheduledExecutor.schedule({
                try {
                    onStart()
                } catch (e: Exception) {
                    channel.sendMessage(ExceptionUtils.getStackTrace(e))
                    e.printStackTrace()
                }
            }, 2, TimeUnit.SECONDS)
            started = true
        }
    }

    /**
     * Logic to run after the [Game] starts. This <b>cannot</b> be called instead of startEvent()
     */
    abstract fun onStart()

    fun cancel(member: Member) {
        cancel(member.user)
    }

    /**
     * Cancel a game (either ending it or during lobby). This should be called in [Game] logic.
     */
    fun cancel(user: User, complain: Boolean = true) {
        if (gamesInLobby.contains(this) || activeGames.contains(this)) {
            gamesInLobby.remove(this)
            activeGames.remove(this)
            if (complain) register.sender.send("**[]** cancelled this game (likely due to no response) or the lobby was open for over 5 minutes ;("
                    .apply(user.display()), null, channel, register.selfUser, null, callback = {
                scheduledExecutor.shutdownNow()
            })
        }
    }

    /**
     * Clean up the game, ending it and inserting the provided [GameData] into the database.
     * @param gameData <b>[Game] specific</b> data class that extends the [GameData] class. This is what is inserted and must be serializable.
     */
    fun cleanup(gameData: GameData) {
        if (activeGames.contains(this)) {
            gamesInLobby.remove(this)
            activeGames.remove(this)
            if (register.database.get("${type.readable}Data", gameId) == null) {
                gameData.id = gameId
                register.database.insert(gameData)
            } else {
                val newGameId = type.findNextId(register)
                gameData.id = gameId
                register.sender.send("This Game ID has already been inserted into the database. Your new Game ID is **[]**"
                        .apply(newGameId), null, channel, register.selfUser, null)
                register.database.insert(gameData)
            }
            register.sender.send("Game Data has been successfully inserted into the database. To view the results and statistics for this match, you can go to https://ardentbot.com/games/[]/[]"
                    .apply(type.name.toLowerCase(), gameId) + "\n\n" +
                    "*Please consider making a small monthly pledge at [] if you enjoyed this game to support our hosting and development costs"
                            .apply("<https://patreon.com/ardent>") + "\n   - Adam*", null, channel, register.selfUser, null)
        }
    }

    private fun announceCreation() {
        if (players.size > 1 && isPublic) {
            register.sender.send("You successfully created a **Public []** game with ID #__[]__!\nAnyone in this server can join by typing */minigames join #[]*"
                    .apply(type.readable, gameId, gameId), null, channel, register.selfUser, null)
        }
    }

    fun translate(id: String): String {
        return register.translationManager.translate(id, channel.guild.getLanguage(register) ?: Language.ENGLISH)
    }
}

enum class GameType(val readable: String, val description: String, val id: Int) {
    BLACKJACK("Blackjack", "this is a placeholder", 2),
    TRIVIA("Trivia", "this is a placeholder", 3),
    BETTING("Betting", "this is a placeholder", 4),
    CONNECT_4("Connect_4", "this is a placeholder", 5),
    TIC_TAC_TOE("Tic_Tac_Toe", "this is a placeholder", 7),
    SLOTS("Slots", "this is a placeholder", 6),
    GUESS_THE_NUMBER("Guess_The_Number", "this is a placeholder", 8);

    override fun toString(): String {
        return readable
    }

    /**
     * Generate a game ID, taking care to avoid duplication of an id.
     */
    fun findNextId(register: ArdentRegister): Long {
        val number = register.random.nextInt(99999) + 1
        return if (register.database.get("${readable}Data", number) == null) number.toLong()
        else findNextId(register)
    }
}

fun Member.isInGameOrLobby(): Boolean {
    return user.isInGameOrLobby()
}

fun Guild.hasGameType(gameType: GameType): Boolean {
    gamesInLobby.forEach { if (it.type == gameType && it.channel.guild.id == id) return true }
    activeGames.forEach { if (it.type == gameType && it.channel.guild.id == id) return true }
    return false
}

fun User.isInGameOrLobby(): Boolean {
    gamesInLobby.forEach { if (it.players.contains(id)) return true }
    activeGames.forEach { if (it.players.contains(id)) return true }
    return false
}

class TriviaPlayerData(var wins: Int = 0, var losses: Int = 0, var questionsCorrect: Int = 0, var questionsWrong: Int = 0, var overallCorrectPercent: Double = 0.0, var percentageCorrect: HashMap<String, Double> = hashMapOf()) {
    fun percentagesFancy(): String {
        val builder = StringBuilder()
        percentageCorrect.forEach { category, percent -> builder.append("  ${Emojis.SMALL_ORANGE_DIAMOND} $category: *${percent.toInt()}*%\n") }
        return builder.toString()
    }
}

class SlotsPlayerData(wins: Int = 0, losses: Int = 0, var netWinnings: Double = 0.0) : PlayerGameData(wins, losses)

class Connect4PlayerData(wins: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, 0)

class BlackjackPlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, ties)

class BettingPlayerData(wins: Int = 0, losses: Int = 0, var netWinnings: Double = 0.0) : PlayerGameData(wins, losses)

class TicTacToePlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, ties)

abstract class PlayerGameData(var wins: Int = 0, var losses: Int = 0, var ties: Int = 0)

class GameDataSlots(gameId: Long, creator: String, startTime: Long, val rounds: List<SlotsGame.Round>) : GameData("SlotsData", gameId, creator, startTime)

class GameDataConnect4(gameId: Long, creator: String, startTime: Long, val winner: String, val loser: String, val game: String)
    : GameData("Connect_4Data", gameId, creator, startTime)

class GameDataBetting(gameId: Long, creator: String, startTime: Long, val rounds: List<BetGame.Round>) : GameData("BettingData", gameId, creator, startTime)

class GameDataBlackjack(gameId: Long, creator: String, startTime: Long, val rounds: List<BlackjackGame.Round>)
    : GameData("BlackjackData", gameId, creator, startTime)

class GameDataTicTacToe(gameId: Long, creator: String, startTime: Long, val playerOne: String, val playerTwo: String, val winner: String?, val game: String)
    : GameData("Tic_Tac_ToeData", gameId, creator, startTime)

class GameDataTrivia(gameId: Long, creator: String, startTime: Long, val winner: String, val losers: List<String>, val scores: Map<String, Int>,
                     val rounds: List<TriviaGame.Round>) : GameData("TriviaData", gameId, creator, startTime) {
    fun sanitize(register: ArdentRegister): SanitizedTrivia {
        val scoresTemp = hashMapOf<String, Int>()
        scores.forEach { t, u -> scoresTemp[t.toUser(register)!!.display()] = u }
        val roundsTemp = mutableListOf<SanitizedTriviaRound>()
        roundsTemp.addAll(rounds.map { (winners, losers1, question) ->
            SanitizedTriviaRound(winners.isNotEmpty(), winners.getOrNull(0)?.toUser(register), losers1.map { it.toUser(register) }, question)
        })

        return SanitizedTrivia(creator.toUser(register)!!, id, winner.toUser(register)!!, losers.map { it.toUser(register)!! },
                scoresTemp.toList().sortedByDescending { it.second }, roundsTemp)
    }
}

abstract class GameData(table: String, id: Long, val creator: String, val startTime: Long, val endTime: Long = System.currentTimeMillis())
    : DbObject(id, table)

data class SanitizedGame(val user: String, val endTime: String, val type: String, val url: String)

fun TextChannel.send(message: String, register: ArdentRegister, post: ((Message) -> Unit)? = null) {
    register.sender.send(message, null, this, this.guild.selfMember.user, null, callback = post)
}

fun TextChannel.send(embed: EmbedBuilder, register: ArdentRegister) {
    register.sender.send(embed, null, this, this.guild.selfMember.user, null)
}

fun User.getData(register: ArdentRegister) = register.database.getUserData(this)