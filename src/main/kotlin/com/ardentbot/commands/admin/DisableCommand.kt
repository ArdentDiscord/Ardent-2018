package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.DisabledCommand
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class DisableCommand : Command("disablecommand", arrayOf("dcommand"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        when {
            arguments.size > 1 -> {
                val command = register.holder.getCommand(arguments[1])
                val exists = command?.name in data.disabledCommands.map { it.name }
                val arg = arguments.getOrNull(0)

                when {
                    arg?.isTranslatedArgument("add", event.guild, register) == true -> {
                        if (command == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + translate("general.specify_valid_command", event, register),
                                this, event)
                        else if (register.holder.getModuleFor(command).name == "admin" || command.name == "help" || command.name == "suggest"
                                || command.name == "disablecommand" || command.name == "disablemodule" || command.name == "audit") {
                            register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                    translate("disablecommand.cannot_disable", event, register), this, event)
                        } else {
                            if (exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                    translate("disablecommand.already_disabled", event, register), this, event)
                            else {
                                data.disabledCommands.add(DisabledCommand(command.name, event.author.id, System.currentTimeMillis()))
                                register.database.update(data)
                                register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                        translate("disablecommand.disabled", event, register).apply(command.name), this, event)
                            }
                        }
                    }
                    arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                        if (command == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + translate("general.specify_valid_command", event, register),
                                this, event)
                        else {
                            if (!exists) register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                                    translate("disablecommand.no_disabled", event, register), this, event)
                            else {
                                data.disabledCommands.removeIf { it.name == command.name }
                                register.database.update(data)
                                register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                        translate("disablecommand.re-enable", event, register).apply(command.name), this, event)
                            }
                        }
                    }
                    else -> displayHelp(event, arguments, flags, register)
                }
            }
            arguments.getOrNull(0)?.isTranslatedArgument("list", event.guild, register) == true -> {
                val embed = getEmbed(translate("disablemodule.title", event, register).apply(event.guild.name), event.author, event.guild)
                if (data.disabledCommands.isEmpty()) embed.appendDescription("No disabled commands")
                else data.disabledCommands.forEach { disabled ->
                    embed.appendDescription(Emojis.SMALL_ORANGE_DIAMOND.cmd
                            + "[], disabled on [] by []".apply(disabled.name, disabled.addDate.localeDate(),
                            disabled.adder.toMember(event.guild)?.user?.display()
                                    ?: translate("unknown", event, register)) + "\n")
                }
                register.sender.cmdSend(embed, this, event)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val add = Argument("add")
    val remove = Argument("remove")
    val list = Argument("list")

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER))

    val example = "add roll"
}