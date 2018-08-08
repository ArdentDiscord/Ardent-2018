package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.DisabledModule
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class DisableModule : Command("disablemodule", arrayOf("dmod", "disablemod"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        if (arguments.size > 1) {
            val module = register.holder.getModule(arguments[1])
            val exists = module?.id in data.disabledModules.map { it.name }
            val arg = arguments.getOrNull(0)
            when {
                arg?.isTranslatedArgument("add", event.guild, register) == true -> {
                    if (module == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to specify a valid module. Use /help for a complete list",
                            this, event)
                    else if (module.id == "admin" || module.id == "info") {
                        register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You cannot disable this module.", this, event)
                    } else {
                        if (exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "This module has already been disabled!", this, event)
                        else {
                            data.disabledModules.add(DisabledModule(module.id, event.author.id, System.currentTimeMillis()))
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Disabled the **[]** module"
                                    .apply(module.id), this, event)
                        }
                    }
                }
                arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                    if (module == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to specify a valid module. Use /help for a complete list",
                            this, event)
                    else {
                        if (!exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "This module hasn't been disabled!", this, event)
                        else {
                            data.disabledModules.removeIf { it.name == module.id }
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Re-enabled the **[]** module".apply(module.name), this, event)
                        }
                    }
                }
                else -> displayHelp(event, arguments, flags, register)
            }
        } else if (arguments.getOrNull(0)?.isTranslatedArgument("list", event.guild, register) == true) {
            val embed = getEmbed("Disabled modules | []".apply(event.guild.name), event.author, event.guild)
            if (data.disabledModules.isEmpty()) embed.appendDescription("There are no disabled modules!")
            else data.disabledModules.forEach { disabled ->
                embed.appendDescription(Emojis.SMALL_ORANGE_DIAMOND.cmd
                        + "**[]** - disabled *[]* by __[]__".apply(disabled.name, disabled.addDate.localeDate(),
                        disabled.adder.toMember(event.guild)?.user?.display() ?: "unknown") + "\n")
            }
            register.sender.cmdSend(embed, this, event)
        } else displayHelp(event, arguments, flags, register)
    }

    val add = Argument("add")
    val remove = Argument("remove")
    val list = Argument("list")

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER))

    val example = "remove fun"
}