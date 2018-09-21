package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class AdminRole : Command("adminrole", arrayOf("adm"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("set", event.guild, register) == true -> {
                if (arguments.size == 1) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + translate("general.specifyrole", event, register),
                        this, event)
                else {
                    if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                        val role = event.guild.getRolesByName(arguments.without(0).concat(), true).getOrNull(0)
                                ?: return event.channel.send(Emojis.CROSS_MARK.cmd + translate("general.norolefound", event, register), register)
                        val old = data.adminRoleId
                        data.adminRoleId = role.id
                        register.database.update(data)
                        event.channel.send(Emojis.HEAVY_CHECK_MARK.cmd + translate("adminrole.updated", event, register).apply("**${role.name}**"), register)
                        if (old != null) {
                            event.channel.send(Emojis.WARNING_SIGN.cmd + translate("adminrole.warning", event, register), register)
                        }
                    }
                }
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    data.adminRoleId = null
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + translate("adminrole.removed", event, register), this, event)
                }
            }
            arg?.isTranslatedArgument("audit", event.guild, register) == true -> {
                val currentRole = data.adminRoleId?.let { event.guild.getRoleById(it) }
                val usersWithRole = currentRole?.let { role -> event.guild.members.filter { it.roles.contains(role) } }
                val embed = getEmbed(translate("adminrole.adminrole", event, register), event.author, event.guild)
                        .appendDescription(translate("adminrole.current", event, register).apply(currentRole?.asMention
                                ?: translate("general.not_set",event, register)) + "\n\n")
                if (usersWithRole != null) {
                    embed.appendDescription(translate("adminrole.userswithcurrent", event, register)
                            .apply(if (usersWithRole.size >= 15) usersWithRole.size else usersWithRole.joinToString { it.asMention }))
                }
                event.channel.send(embed, register)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val set = Argument("set")
    val remove = Argument("remove")
    val audit = Argument("audit")
}