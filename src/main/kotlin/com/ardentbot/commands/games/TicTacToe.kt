package com.ardentbot.commands.games

import com.ardentbot.core.*
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.util.*

@ModuleMapping("games")
class TicTacToeCommand : Command("tictactoe", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val member = event.member!!
        val channel = event.channel
        if (member.isInGameOrLobby()) event.channel.send(translate("games.already_in_game", event, register).apply(member.asMention), register)
        // restrict 1 game per server based on patreon support - disable for now
        /*   else if (event.guild.hasGameType(GameType.TIC_TAC_TOE) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
             channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Tic Tac Toe", "<https://ardentbot.com/patreon>"))
         }*/
        else {
            val game = TicTacToeGame(channel, member.user.id, register)
            gamesInLobby.add(game)
        }

    }
}

class TicTacToeGame(channel: TextChannel, creator: String, register: ArdentRegister) : Game(GameType.TIC_TAC_TOE, channel, creator, 2, false, register) {
    override fun onStart() {
        doRound(Board(players[0], players[1]), players[0])
    }

    private fun doRound(board: Board, player: String, cancelIfExpire: Boolean = false) {
        val member = channel.guild.getMemberById(player)!!
        channel.sendMessage(getEmbed(translate("tictactoe.embed_title"), channel, Color.WHITE)
                .appendDescription(translate("tictactoe.prompt").apply(member.asMention) + "\n\n")
                .appendDescription(board.toString()).build()).queue { message ->
            message.addReaction(Emojis.NORTH_WEST_ARROW.symbol).queue()
            message.addReaction(Emojis.UPWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emojis.NORTHEAST_ARROW.symbol).queue()

            message.addReaction(Emojis.LEFTWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emojis.SMALL_ORANGE_DIAMOND.symbol).queue()
            message.addReaction(Emojis.BLACK_RIGHTWARDS_ARROW.symbol).queue()

            message.addReaction(Emojis.SOUTH_WEST_ARROW.symbol).queue()
            message.addReaction(Emojis.DOWNWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emojis.SOUTHEAST_ARROW.symbol).queue()

            Sender.waitForReaction({ it.user.id == player && it.messageId == message.id && it.guild.id == channel.guild.id }, {
                val messageReaction = it.reaction

                val place: Int? = when (messageReaction.reactionEmote.name) {
                    Emojis.NORTH_WEST_ARROW.symbol -> 1
                    Emojis.UPWARDS_BLACK_ARROW.symbol -> 2
                    Emojis.NORTHEAST_ARROW.symbol -> 3
                    Emojis.LEFTWARDS_BLACK_ARROW.symbol -> 4
                    Emojis.SMALL_ORANGE_DIAMOND.symbol -> 5
                    Emojis.BLACK_RIGHTWARDS_ARROW.symbol -> 6
                    Emojis.SOUTH_WEST_ARROW.symbol -> 7
                    Emojis.DOWNWARDS_BLACK_ARROW.symbol -> 8
                    Emojis.SOUTHEAST_ARROW.symbol -> 9
                    else -> null
                }
                if (place == null) {
                    channel.send(translate("tictactoe.invalid_input").apply(member.asMention), register)
                    if (!cancelIfExpire) doRound(board, player, true)
                    else cancel(member)
                } else {
                    if (board.put(place, player == players[0])) {
                        val winner = board.checkWin()
                        if (winner == null) {
                            if (board.spacesLeft().size > 0) doRound(board, if (player == players[0]) players[1] else players[0])
                            else {
                                message.editMessage(getEmbed(translate("tictactoe.embed_title"), channel, Color.WHITE)
                                        .appendDescription(translate("tictactoe.game_tied") + " :(\n\n")
                                        .appendDescription(board.toString()).build()).queue()
                                doCleanup(board, null)
                            }
                        } else {
                            message.editMessage(getEmbed(translate("tictactoe.embed_title"), channel, Color.WHITE)
                                    .appendDescription(translate("tictactoe.game_over").apply(winner.toUser(register)?.asMention
                                            ?: translate("unknown")) + "\n\n")
                                    .appendDescription(board.toString()).build()).queue()
                            doCleanup(board, winner)
                        }
                    } else {
                        channel.send(translate("tictactoe.invalid_input").apply(member.asMention), register)
                        if (!cancelIfExpire) doRound(board, player, true)
                        else cancel(member)
                    }
                }
                message.delete().queue()
            }, {
                if (cancelIfExpire) cancel(member)
                else {
                    channel.send(translate("tictactoe.no_input").apply(member.asMention), register)
                    doRound(board, player, true)
                }
                message.delete().queue()
            }, 45)
        }
    }

    private fun doCleanup(board: Board, winner: String?) {
        Thread.sleep(2000)
        cleanup(GameDataTicTacToe(gameId, creator, startTime!!, players[0], players[1], winner, board.toString()))
        val creatorMember = channel.guild.getMemberById(creator)!!
        channel.selectFromList(creatorMember, translate("tictactoe.start_again"), mutableListOf(translate("yes"), translate("no")), { selection, selectionMessage ->
            if (selection == 0) {
                channel.send(translate("tictactoe.creating"), register)
                val newGame = TicTacToeGame(channel, creatorMember.user.id, register)
                gamesInLobby.add(newGame)
            } else channel.send(translate("tictactoe.no_continue"), register)
            selectionMessage.delete().queue()
        }, footer = translate("tictactoe.only_creator"), register = register)
    }

    data class Board(val playerOne: String, val playerTwo: String, val tiles: Array<String?> = Array(9) { null }) {
        fun put(space: Int, isPlayerOne: Boolean): Boolean {
            return if (space !in 1..9 || tiles[space - 1] != null) false
            else {
                tiles[space - 1] = if (isPlayerOne) playerOne else playerTwo
                true
            }
        }

        fun checkWin(): String? {
            (0..2)
                    .filter { tiles[0 + 3 * it] != null && tiles[0 + 3 * it] == tiles[1 + 3 * it] && tiles[0 + 3 * it] == tiles[2 + 3 * it] }
                    .forEach { return tiles[0 + 3 * it] }
            (0..2)
                    .filter { tiles[it] != null && tiles[it] == tiles[it + 3] && tiles[it] == tiles[it + 6] }
                    .forEach { return tiles[it] }
            if (tiles[0] != null && tiles[0] == tiles[4] && tiles[0] == tiles[8]) return tiles[0]
            if (tiles[2] != null && tiles[2] == tiles[4] && tiles[2] == tiles[6]) return tiles[2]
            return null
        }

        fun spacesLeft(): MutableList<Int> {
            val list = mutableListOf<Int>()
            tiles.forEachIndexed { index, s -> if (s == null) list.add(index) }
            return list
        }

        override fun toString(): String {
            val builder = StringBuilder()
                    .append("▬▬▬▬▬▬▬▬▬▬\n▐ ")
            tiles.forEachIndexed { index, s ->
                builder.append(when (s) {
                    playerOne -> "❌"
                    playerTwo -> "⭕"
                    else -> "⬛"
                })
                if ((index + 1) % 3 == 0) {
                    builder.append("▐ \n▬▬▬▬▬▬▬▬▬▬\n")
                    if (index != 8) builder.append("▐ ")
                } else builder.append(" ║ ")
            }
            return builder.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Board
            if (playerOne != other.playerOne) return false
            if (playerTwo != other.playerTwo) return false
            if (!Arrays.equals(tiles, other.tiles)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = playerOne.hashCode()
            result = 31 * result + playerTwo.hashCode()
            result = 31 * result + Arrays.hashCode(tiles)
            return result
        }
    }
}
