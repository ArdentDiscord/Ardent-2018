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
            when {
                arguments.isNotEmpty() -> {
                    val command = register.holder.commands.firstOrNull {
                        it.name == arguments[0] || it.aliases?.contains(arguments[0]) == true
                    }
                    when {
                        command == null -> {
                            val module = register.holder.modules.entries.find {
                                it.key.name.equals(arguments[0], true) || it.key.id.equals(arguments[0], true)
                                        || arguments[0].isTranslatedPhrase("module.${it.key.name}", event.guild, register)
                            }
                            if (module != null) return onInvoke(event, listOf(), listOf(Flag("m", "\"${module.key.name}\"")), register)
                            register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                    translate("help.nocommandwithname", event, register).apply(arguments[0]), this, event)
                        }
                        register.database.getGuildData(event.guild).disabledModules.map { it.name }.contains(register.holder.getModuleFor(command).id) -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd
                                + translate("help.moduleisdisabled", event, register).apply(register.holder.getModuleFor(command).name), this, event)
                        else -> command.displayHelp(event, arguments, flags, register)
                    }
                }
                flags.get("m") != null -> {
                    val flag = flags.get("m")!!
                    try {
                        val module = register.holder.modules.keys.first { it.name.equals(flag.value, true) || it.id.equals(flag.value, true) }
                        val data = register.database.getGuildData(event.guild)
                        if (data.disabledModules.map { it.name }.contains(module.id)) {
                            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    translate("help.moduleisdisabled", event, register).apply(module.name), this, event)
                            return
                        }
                        val embed = getEmbed(module.name + " | " + translate("help.module_help", event, register)
                                , event.author, event.guild)
                                .appendDescription("**${translate("help.command_list", event, register)}**")
                                .appendDescription("\n")
                        register.holder.modules[module]!!.forEachIndexed { i, command ->
                            embed.appendDescription("   ${i.diamond()} **" + command.getTranslatedName(event.guild, register) + "**: "
                                    + (command.getTranslatedDescription(event.guild, register)
                                    ?: translate("help.no_description_available", event, register)))
                            if (command.aliases?.isNotEmpty() == true) {
                                embed.appendDescription("\n     " + translate("help.command_aliases", event, register)
                                        .apply("*${command.aliases.joinToString()}*"))
                            }
                            embed.appendDescription("\n")
                        }
                        register.sender.cmdSend(embed, this, event)
                    } catch (e: NoSuchElementException) {
                        register.sender.cmdSend(Emojis.CROSS_MARK.cmd + translate("help.no_module_with_name", event, register),
                                this, event)
                    }
                }
                else -> displayHelp(event, arguments, flags, register)
            }
        }
    }

    override fun displayHelp(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed(translate("help.embed_title", event, register), event.author, event.guild)
                .appendDescription(translate("help.whats_new", event, register))
                .appendDescription("\n\n")
                .appendDescription("__" + translate("help.modules", event, register) + "__:" + " "
                        + register.holder.modules.keys.sortedBy { it.name }.joinToString { "`${translate("module.${it.name}", event, register)} (${it.id})`" })
                .appendDescription("\n\n")
        val data = register.database.getGuildData(event.guild)
        register.holder.modules.filter { !data.disabledModules.map { disabledModule -> disabledModule.name }.contains(it.key.id) }.toList()
                .sortedBy { it.first.name }.forEach { (module, moduleCommands) ->
                    embed.appendDescription("**__${translate("module.${module.name}", event, register)}__** ").appendDescription("`")
                            .appendDescription(moduleCommands.sortedBy { it.name }.joinToString { translate(it.name, event, register) })
                    embed.appendDescription("`\n\n")
                }
        embed.appendDescription(translate("help.gethelp", event, register))
                .appendDescription("\n")
                .appendDescription(translate("help.see_module", event, register))

        register.sender.cmdSend(embed, this, event)
    }
}