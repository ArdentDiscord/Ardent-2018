package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.commands.Command
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class AdminRole:Command("adminrole", arrayOf("adm"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        // TODO prompt to switch over all existing admin role members to a new one
        val data = register.database.getGuildData(event.guild)
        when(arguments.getOrNull(0)) {
            "set" -> {
                if (arguments.size == 1) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to specify a role to set as the admin role!",
                        this, event)
                else {
                    if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                        //
                    }
                }
            }
            "remove" -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    data.adminRoleId = null
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Removed the admin role", this, event)
                }
            }
            "audit" -> {

            }
            else -> displayHelp(event, arguments, flags, register)
        }

    }

    val set = ArgumentInformation("set [role name]", "set the Ardent admin role in your server")
    val remove = ArgumentInformation("remove", "removes the currently set role from being the admin role")
    val audit = ArgumentInformation("audit", "see what's set as the admin role (and who has that role)")
}