package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.get
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.diamond
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class Help : Command("help", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty() && flags.isEmpty()) displayHelp(event, arguments, flags, register)
        else {
            if (arguments.isNotEmpty()) {
                val command = register.holder.commands.firstOrNull {
                    it.name == arguments[0] || it.aliases?.contains(arguments[0]) == true
                }
                when {
                    command == null -> {
                        val module = register.holder.modules.entries.find {
                            it.key.name.equals(arguments[0], true) || it.key.id.equals(arguments[0], true)
                        }
                        if (module != null) return onInvoke(event, listOf(), listOf(Flag("m", "\"${module.key.name}\"")), register)
                        register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                "I couldn't find a command with the name **[]**".apply(arguments[0]), this, event)
                    }
                    register.database.getGuildData(event.guild).disabledModules.map { it.name }.contains(register.holder.getModuleFor(command).id) -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Your server administrators have " +
                            "chosen to disable the **[]** module in Ardent".apply(register.holder.getModuleFor(command).name), this, event)
                    else -> command.displayHelp(event, arguments, flags, register)
                }
            } else if (flags.get("m") != null) {
                val flag = flags.get("m")!!
                try {
                    val module = register.holder.modules.keys.first { it.name.equals(flag.value, true) || it.id.equals(flag.value, true) }
                    val data = register.database.getGuildData(event.guild)
                    if (data.disabledModules.map { it.name }.contains(module.id)) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Your server administrators have " +
                                "chosen to disable the **[]** module in Ardent".apply(module.name), this, event)
                        return
                    }
                    val embed = getEmbed(module.name + " | " + "Module Help", event.author, event.guild)
                            .appendDescription("**Commands**")
                            .appendDescription("\n")
                    register.holder.modules[module]!!.forEachIndexed { i, command ->
                        embed.appendDescription("   ${i.diamond()} **" + command.name + "**: "
                                + (command.getTranslatedDescription(event.guild, register)
                                ?: "no description available for this command"))
                        if (command.aliases?.isNotEmpty() == true) {
                            embed.appendDescription("\n     aliases: *${command.aliases.joinToString()}*")
                        }
                        embed.appendDescription("\n")
                    }
                    register.sender.cmdSend(embed, this, event)
                } catch (e: NoSuchElementException) {
                    register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "I couldn't find a module by that name", this, event)
                }
            } else displayHelp(event, arguments, flags, register)
        }
    }

    override fun displayHelp(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed("Ardent | Command Overview", event.author, event.guild)
                .appendDescription("To learn what's *changed*, *been improved* and what **flags** and " +
                        "**arguments** are, type _ardent 2018_")
                .appendDescription("\n\n")
                .appendDescription("__Modules__:" + " "
                        + register.holder.modules.keys.sortedBy { it.name }.joinToString { "`${it.name} (${it.id})`" })
                .appendDescription("\n\n")
        val data = register.database.getGuildData(event.guild)
        register.holder.modules.filter { !data.disabledModules.map { it.name }.contains(it.key.id) }.toList()
                .sortedBy { it.first.name }.forEach { (module, moduleCommands) ->
                    embed.appendDescription("**__${module.name}__** ").appendDescription("`")
                            .appendDescription(moduleCommands.sortedBy { it.name }.joinToString { it.name })
                    embed.appendDescription("`\n\n")
                }
        embed.appendDescription("To get help on a specific command, type _/help **command**_.")
                .appendDescription("\n")
                .appendDescription("To see a module's commands, type _/help -m **module**_")

        register.sender.cmdSend(embed, this, event)
    }
}