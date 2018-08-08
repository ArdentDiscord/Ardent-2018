package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import com.ardentbot.core.database.ArdentPrefix
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class Prefix : Command("prefix", arrayOf("p"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) {
            displayHelp(event, arguments, flags, register)
            return
        }

        val data = register.database.getGuildData(event.guild)

        when (arguments[0]) {
            "add", "remove" -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size > 1) {
                        val prefix = arguments.without(0).joinToString(" ")
                        val exists = data.prefixes.firstOrNull { it.prefix == prefix } != null
                        when (arguments[0]) {
                            "add" -> {
                                if (exists) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "There's already a prefix by that name!",
                                        this, event)
                                else {
                                    data.prefixes.add(ArdentPrefix(prefix, event.author.id, System.currentTimeMillis()))
                                    register.database.update(data)
                                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Added **[]** to **[]**'s prefixes"
                                            .apply(prefix, event.guild.name), this, event)
                                }
                            }
                            "remove" -> {
                                if (!exists) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "There's **not** a prefix by that name!",
                                        this, event)
                                else {
                                    data.prefixes.removeIf { it.prefix == prefix }
                                    register.database.update(data)
                                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Removed **[]** from **[]**'s prefixes"
                                            .apply(prefix, event.guild.name), this, event)
                                }
                            }
                        }
                    } else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to include a prefix to []"
                            .apply(arguments[0]), this, event)
                }
            }
            "list" -> {
                val embed = getEmbed("Prefix List | []".apply(event.guild.name), event.author, event.guild)
                        .appendDescription("**Prefixes**:")
                data.prefixesModified(register).forEach { prefix ->
                    embed.appendDescription("\n")
                            .appendDescription("**[]** > *added by [] on []*".apply(prefix.prefix,
                                    prefix.adder.toMember(event.guild)?.user?.display() ?: "unknown",
                                    prefix.addDate.localeDate()))
                }
                register.sender.cmdSend(embed, this, event)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val add = ArgumentInformation("add", "add a new prefix for Ardent")
    val remove = ArgumentInformation("remove", "remove an existing prefix for Ardent")
    val list = ArgumentInformation("list", "list all current Ardent prefixes for this server")

    val example = "add !"
}