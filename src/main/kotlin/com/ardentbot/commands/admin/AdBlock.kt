package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
@MockCommand("stop users from advertising other discord servers!")
@MockTranslations(
        MockTr("")
)
class AdBlock:Command("adblock", arrayOf("antiadvertise"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val arg = arguments.getOrNull(0)
        when  {
            arg != null && arg.isTranslatedArgument("settings", event.guild,register) -> {

            }
            arg != null && arg.isTranslatedArgument("set", event.guild,register) -> {

            }
            arg != null && arg.isTranslatedArgument("remove", event.guild,register) -> {

            }
        }
    }

    val settings = Argument("settings")
    val set = Argument("set")
    val remove = Argument("remove")
}