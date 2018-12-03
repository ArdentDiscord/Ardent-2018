package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
@MockCommand("set a chat cooldown for your server - the amount of time between messages")
@MockArguments(
        MockArgument("remove", "remove the existing chat cooldown"),
        MockArgument("set", "set the cooldown time between messages (in seconds)", "set [cooldown in seconds]")
)
@MockTranslations(
        MockTr("remove_success", "{WHITE_HEAVY_CHECKMARK} Successfully removed the chat cooldown"),
        MockTr("set_success", "{WHITE_HEAVY_CHECKMARK} Successfully set the chat cooldown to **[]** seconds"),
        MockTr("need_cooldown_time", "You need to add a cooldown time!"),
        MockTr("time_info", "The current cooldown time is **[]** seconds"),
        MockTr("not_set", "There is no cooldown time set."),
        MockTr("set", "Set it with /antispam set [cooldown time]"),
        MockTr("blocked", "")
)
class Antispam : Command("antispam", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)

        when {
            // remove
            arguments.isNotEmpty() && arguments[0].equals(translate("adblock.arguments.remove", event, register), true) -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    data.antispamCooldownSeconds = null
                    register.database.update(data)
                    event.channel.send(translate("antispam.remove_success", event, register), register)
                }
            }
            // set
            arguments.isNotEmpty() && arguments[0].equals(translate("adblock.arguments.set", event, register), true) -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1 || arguments[1].toIntOrNull() == null) event.channel.send(translate("antispam.need_cooldown_time", event, register), register)
                    else {
                        val time = arguments[1].toInt()
                        data.antispamCooldownSeconds = time
                        register.database.update(data)
                        event.channel.send(translate("antispam.set_success", event, register).apply(time), register)
                    }
                }
            }
            // default
            else -> {
                val sb = StringBuilder()
                if (data.antispamCooldownSeconds == null) sb.append(translate("antispam.not_set",event, register))
                else sb.append(translate("antispam.time_info",event, register).apply(data.antispamCooldownSeconds!!))
                sb.append("\n").append(translate("antispam.set",event, register))
                event.channel.send(sb.toString(), register)
            }
        }
    }
}