package com.ardentbot.commands.rpg

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.getUsersData
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class TopMoney : Command("top", arrayOf("topmoney"), 5) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val isAll = arguments.getOrNull(0)?.isTranslatedArgument("all", event.guild, register) == true
        val isServer = arguments.getOrNull(0)?.isTranslatedArgument("server", event.guild, register) == true
        if (!isAll && !isServer) displayHelp(event, arguments, flags, register)
        else {
            val server = isServer
            val pageLimit = Math.ceil((if (server) event.guild.members.size else register.database.getUsersSize().toInt()).div(10.0)).toInt()
            PaginationEmbed(event.author.id, { page ->
                val embed = getEmbed((if (server) translate("top.global", event, register) else translate("top.local", event, register).apply(event.guild.name)) +
                        " - " + translate("page", event, register).apply(page), event.author, event.guild)
                        .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
                val users: List<Pair<User, Long>> = (if (server) {
                    event.guild.getUsersData(register).let { data ->
                        data.sortedByDescending { it.money }.subList((page - 1) * 10,
                                if (data.size >= (page - 1) * 10 + 10) ((page - 1) * 10 + 10) else data.size)
                    }
                } else register.database.getRichestUsers(page)).map { register.getUser(it.id as String) to it.money }.filterNot { it.first == null }
                        .map { it.first!! to it.second }
                users.forEachIndexed { i, pair ->
                    embed.appendDescription("#${((page - 1) * 10) + i + 1}: **${pair.first.display()}** $" + pair.second.format() + "\n")
                }
                embed
            }, MAX_PAGE(pageLimit), register = register).send(event.channel)
        }
    }

    val server = Argument("server")
    val all = Argument("all")
}
