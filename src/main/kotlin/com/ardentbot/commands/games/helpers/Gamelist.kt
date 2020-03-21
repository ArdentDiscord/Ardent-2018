package com.ardentbot.commands.games.helpers

import com.ardentbot.commands.games.activeGames
import com.ardentbot.commands.games.gamesInLobby
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.kotlin.toUsersDisplay
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("games")
class Gamelist : Command("gamelist", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed(translate("gamelist.embed_title", event, register), event.author, event.guild)
        val builder = StringBuilder()
                .append("**" + translate("gamelist.info", event, register) + "**")
        if (gamesInLobby.isEmpty() && activeGames.isEmpty()) register.sender.cmdSend("\n\n" +
                translate("gamelist.no_games", event, register), this, event)
        else {
            gamesInLobby.forEach {
                builder.append("\n\n ${Emojis.LARGE_RED_CIRCLE}")
                        .append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] " + "created by" + " __${it.creator.toUser(register)?.display()}__ | ${it.players.toUsersDisplay(register)}")
            }
            activeGames.forEach {
                builder.append("\n\n ${Emojis.LARGE_GREEN_CIRCLE}")
                        .append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] " + "created by" + " __${it.creator.toUser(register)?.display()}__ | ${it.players.toUsersDisplay(register)}")
            }

            //builder.append("\n\n" + "__Take Note__: You can run only one game of each type at a time in this server unless you become an Ardent patron")

            embed.setDescription(builder.toString())
            register.sender.cmdSend(embed, this, event)
        }
    }
}