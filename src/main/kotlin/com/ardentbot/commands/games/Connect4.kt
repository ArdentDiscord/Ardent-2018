package com.ardentbot.commands.games

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

class Connect4Game(channel: TextChannel, creator: String, register: ArdentRegister)
    : Game(GameType.CONNECT_4, channel, creator, 2, false, register) {
    override fun onStart() {
        val first = if(register.random.nextBoolean()) players[0] else players[1]
        val game = GameBoard(first, if (first == players[0]) players[1] else players[0])
        doRound(game, channel.guild.getMemberById(first)!!)
    }

    private fun doRound(game: GameBoard, player: Member, cancelIfExpired: Boolean = false) {
        if (game.full()) {
            channel.send(translate("connect4.game_tied").apply(player.asMention), register)
            val embed = getEmbed(translate("connect4.result_title"), channel, Color.BLUE)
            embed.appendDescription(translate("connect4.winner_tied").apply(player.asMention) + "\n$game")
            channel.send(embed, register)
            cleanup(GameDataConnect4(gameId, creator, startTime!!, player.user.id, if (player.user.id == players[0]) players[1] else players[0], game.toString()))
        } else {
            val embed = getEmbed(translate("connect4.board_title"), channel, Color.BLUE)
            embed.appendDescription(translate("connect4.prompt").apply(player.asMention,
                    if (players[0] == player.user.id) "\uD83D\uDD34" else "\uD83D\uDD35") + "\n")
            embed.appendDescription(game.toString())
            channel.sendMessage(embed.build()).queue { message ->
                message.addReaction(Emojis.KEYCAP_DIGIT_ONE.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_TWO.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_THREE.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_FOUR.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_FIVE.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_SIX.symbol).queue()
                message.addReaction(Emojis.KEYCAP_DIGIT_SEVEN.symbol).queue()

                Sender.waitForReaction({ it.user.id == player.user.id && it.messageId == message.id }, {
                    when (it.reactionEmote.name) {
                        Emojis.KEYCAP_DIGIT_ONE.symbol -> place(0, game, player)
                        Emojis.KEYCAP_DIGIT_TWO.symbol -> place(1, game, player)
                        Emojis.KEYCAP_DIGIT_THREE.symbol -> place(2, game, player)
                        Emojis.KEYCAP_DIGIT_FOUR.symbol -> place(3, game, player)
                        Emojis.KEYCAP_DIGIT_FIVE.symbol -> place(4, game, player)
                        Emojis.KEYCAP_DIGIT_SIX.symbol -> place(5, game, player)
                        Emojis.KEYCAP_DIGIT_SEVEN.symbol -> place(6, game, player)
                        else -> {
                            channel.send(translate("connect4.invalid_input").apply(player.asMention), register)
                            doRound(game, player, true)
                        }
                    }
                    message.delete().queue()
                }, {
                    if (cancelIfExpired) cancel(creator.toUser(register)!!)
                    else {
                        channel.send(translate("connect4.no_input").apply(player.asMention),register)
                        message.delete().queue()
                        doRound(game, player, true)
                    }
                }, 40, TimeUnit.SECONDS)
            }
        }
    }

    private fun place(column: Int, game: GameBoard, player: Member) {
        val tile = game.put(column, player.user.id == players[0])
        if (tile == null) {
            channel.send(translate("connect4.invalid_column").apply(player.asMention), register)
            doRound(game, player, false)
        } else {
            val winnerId = game.checkWin(tile)
            if (winnerId == null) doRound(game, if (player.user.id == players[0]) channel.guild.getMemberById(players[1])!! else channel.guild.getMemberById(players[0])!!)
            else {
                val winner = channel.guild.getMemberById(winnerId)!!
                val embed = getEmbed(translate("connect4.result_title"), channel, Color.BLUE)
                embed.appendDescription(translate("connect4.winner_congrats").apply(winner.asMention, 500) + "\n")
                embed.appendDescription(game.toString())
                channel.send(embed, register)
                val data = winner.user.getData(register)
                data.money += 500
                register.database.update(data)
                cleanup(GameDataConnect4(gameId, creator, startTime!!, winner.user.id, if (winnerId == players[0]) players[1] else players[0], game.toString()))
            }
        }
    }

    data class Column(val game: GameBoard, val number: Int, val tiles: Array<Tile> = Array(6) { Tile(game, number, it) }) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Column

            if (number != other.number) return false
            if (!Arrays.equals(tiles, other.tiles)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = number
            result = 31 * result + Arrays.hashCode(tiles)
            return result
        }
    }

    data class Tile(val game: GameBoard, val x: Int, val y: Int, var possessor: String? = null) {
        override fun toString(): String {
            return when (possessor) {
                null -> "⚪"
                game.playerOne -> "\uD83D\uDD34"
                else -> "\uD83D\uDD35"
            }
        }
    }

    data class GameBoard(val playerOne: String, val playerTwo: String, val state: GameState = GameState.WAITING_PLAYER_ONE) {
        private val grid: List<Column> = listOf(Column(this, 0), Column(this, 1), Column(this, 2), Column(this, 3), Column(this, 4), Column(this, 5), Column(this, 6))

        fun full(): Boolean {
            grid.forEach { grid -> grid.tiles.forEach { tile -> if (tile.possessor == null) return false } }
            return true
        }

        fun put(column: Int, playerOne: Boolean): Tile? {
            if (column !in 0..6) return null
            grid[column].tiles.forEach { tile ->
                if (tile.possessor == null) {
                    tile.possessor = if (playerOne) this.playerOne else playerTwo
                    return tile
                }
            }
            return null
        }

        private fun getRow(index: Int): ArrayList<Tile> {
            if (index !in 0..5) throw IllegalArgumentException("Row does not exist at index $index")
            else {
                val row = arrayListOf<Tile>()
                grid.forEach { column -> row.add(column.tiles[index]) }
                return row
            }
        }

        private fun getTile(x: Int, y: Int): Tile? {
            return grid.getOrNull(x)?.tiles?.getOrNull(y)
        }

        private fun getDiagonal(tile: Tile?, left: Boolean): MutableList<Tile> {
            if (tile == null) return mutableListOf()
            var xStart = tile.x
            var yStart = tile.y
            while (yStart > 0 && if (left) xStart > 0 else xStart < 6) {
                if (left) xStart-- else xStart++
                yStart--
            }
            val tiles = mutableListOf<Tile>()
            var found = true
            while (found) {
                val t = getTile(xStart, yStart)
                if (t == null) found = false
                else {
                    tiles.add(t)
                    if (left) xStart++ else xStart--
                    yStart++
                }
            }
            return tiles
        }

        fun checkWin(tile: Tile): String? {
            val row = getRow(tile.y)
            var counter = 0
            var currentOwner: String? = null
            row.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            counter = 0
            val column = grid[tile.x]
            column.tiles.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            val diagonalLeft = diagonal(tile, true)
            if (diagonalLeft != null) return diagonalLeft
            val diagonalRight = diagonal(tile, false)
            if (diagonalRight != null) return diagonalRight
            return null
        }

        private fun diagonal(tile: Tile, direction: Boolean /* True is left, false is right */): String? {
            val tiles = if (direction) getDiagonal(tile, true) else getDiagonal(tile, false)
            var counter = 0
            var currentOwner: String? = null
            tiles.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            return null
        }

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append(" ▬▬▬▬▬▬▬▬▬▬▬ ▬▬▬▬▬▬▬▬▬▬▬")
            builder.append("\n")
            for (rowValue in 5 downTo 0) {
                builder.append("▐ ")
                grid.forEachIndexed { index, it ->
                    builder.append(it.tiles[rowValue])
                    if (index < grid.size - 1) builder.append(" ║ ")
                }
                builder.append("▐\n")
                builder.append(" ▬▬▬▬▬▬▬▬▬▬▬ ▬▬▬▬▬▬▬▬▬▬▬")
                builder.append("\n")
            }
            return builder.toString()
        }
    }

    enum class GameState {
        WAITING_PLAYER_ONE, WAITING_PLAYER_TWO
    }
}

@ModuleMapping("games")
class Connect4Command : Command("connect4", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val member = event.member!!
        if (member.isInGameOrLobby()) event.channel.send(translate("games.already_in_game", event, register).apply(member.asMention), register)
        /*else if (event.guild.hasGameType(GameType.CONNECT_4) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Connect 4", "<https://ardentbot.com/patreon>"))
        } */
        else {
            val game = Connect4Game(event.channel, member.user.id, register)
            gamesInLobby.add(game)
        }
    }
}