package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("settings")
@Excluded
class Log : Command("log", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "set" -> {

            }
            "remove" -> {

            }
            "addevent" -> {
                // needs a -a tag for all events
            }
            "removeevent" -> {

            }
            "audit" -> {

            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.VIEW_AUDIT_LOGS))

    // val add = ArgumentInformation()
}