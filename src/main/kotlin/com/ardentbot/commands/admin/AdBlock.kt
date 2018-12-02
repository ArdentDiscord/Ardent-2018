package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import com.ardentbot.core.database.AntiAdvertisingSettings
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.kotlin.without
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
@MockCommand("stop users from advertising other discord servers!")
@MockArguments(
        MockArgument("settings", "view the current adblock settings"),
        MockArgument("set", "set an adblock parameter", "set [links/ban] [yes/no]"),
        MockArgument("remove", "stop blocking advertisements")
)
@MockTranslations(
        MockTr("remove_success", "{WHITE_HEAVY_CHECKMARK} Successfully removed any existing adblock settings"),
        MockTr("allow_server_links", "Allow server links"),
        MockTr("ban_after_two_infractions", "Ban after 2 infractions"),
        MockTr("how_change", "Change these settings with /adblock set [allow/ban] yes/no"),
        MockTr("allow", "allow"),
        MockTr("ban", "ban")
)
class AdBlock : Command("adblock", arrayOf("antiadvertise"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        val arg = arguments.getOrNull(0)
        when {
            arg != null && arg.isTranslatedArgument("settings", event.guild, register) -> {
                if (data.antiAdvertisingSettings == null) data.antiAdvertisingSettings = AntiAdvertisingSettings(true, false)
                val embed = getEmbed(translate("adblock.setting_embed_title", event, register), event.channel)
                        .appendDescription("**" + translate("adblock.allow_server_links", event, register) + "**: " +
                                translate(if (data.antiAdvertisingSettings!!.allowServerLinks) "yes" else "no", event, register))
                        .appendDescription("\n\n")
                        .appendDescription("**" + translate("adblock.ban_after_two_infractions", event, register) + "**: " +
                                translate(if (data.antiAdvertisingSettings!!.banAfterTwoInfractions) "yes" else "no", event, register))
                        .appendDescription("\n\n")
                        .appendDescription(translate("adblock.how_change", event, register))

                event.channel.send(embed, register)
            }
            arg != null && arg.isTranslatedArgument("set", event.guild, register) -> {
                val parameters = arguments.without(0)
                if (parameters.size != 2) event.channel.send(translate("adblock.how_change", event, register), register)
                else if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val num = if (parameters[0].equals(translate("adblock.allow", event, register), true)) 0
                    else if (parameters[0].equals(translate("adblock.ban", event, register), true)) 1
                    else 2

                }
            }
            arg != null && arg.isTranslatedArgument("remove", event.guild, register) -> {
                if (invokePrecondition((ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER))), event, arguments, flags, register)) {
                    data.antiAdvertisingSettings = null
                    register.database.update(data)
                    event.channel.send(translate("adblock.remove_success", event.guild, register), register)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    //val settings = Argument("settings")
    //val set = Argument("set")
    //val remove = Argument("remove")
}