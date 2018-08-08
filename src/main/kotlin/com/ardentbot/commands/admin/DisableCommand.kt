package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.DisabledCommand
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class DisableCommand : Command("disablecommand", arrayOf("dcommand"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        if (arguments.size > 1) {
            val command = register.holder.getCommand(arguments[1])
            val exists = command?.name in data.disabledCommands.map { it.name }
            when (arguments.getOrNull(0)) {
                "add" -> {
                    if (command == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to specify a valid command. Use /help for a complete list",
                            this, event)
                    else if (register.holder.getModuleFor(command).name == "admin" || command.name == "help" || command.name == "suggest") {
                        register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You cannot disable this command.", this, event)
                    } else {
                        if (exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "This command has already been disabled!", this, event)
                        else {
                            data.disabledCommands.add(DisabledCommand(command.name, event.author.id, System.currentTimeMillis()))
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Disabled the **[]** command".apply(command.name), this, event)
                        }
                    }
                }
                "remove" -> {
                    if (command == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to specify a valid command. Use /help for a complete list",
                            this, event)
                    else {
                        if (!exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "This command hasn't been disabled!", this, event)
                        else {
                            data.disabledCommands.removeIf { it.name == command.name }
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Re-enabled the **[]** command".apply(command.name), this, event)
                        }
                    }
                }
                else -> displayHelp(event, arguments, flags, register)
            }
        } else if (arguments.getOrNull(0) == "list") {
            val embed = getEmbed("Disabled commands | []".apply(event.guild.name), event.author, event.guild)
            if (data.disabledCommands.isEmpty()) embed.appendDescription("There are no disabled commands!")
            else data.disabledCommands.forEach { disabled ->
                embed.appendDescription(Emojis.SMALL_ORANGE_DIAMOND.cmd
                        + "**[]** - disabled *[]* by __[]__".apply(disabled.name, disabled.addDate.localeDate(),
                        disabled.adder.toMember(event.guild)?.user?.display() ?: "unknown") + "\n")
            }
            register.sender.cmdSend(embed, this, event)
        } else displayHelp(event, arguments, flags, register)
    }

    val add = ArgumentInformation("add [command]", "add a command to the disabled commands list")
    val remove = ArgumentInformation("remove [command]", "remove a command from the disabled commands list")
    val list = ArgumentInformation("list", "list all the currently disabled commands in this server")

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER))

    val example = "add roll"
}