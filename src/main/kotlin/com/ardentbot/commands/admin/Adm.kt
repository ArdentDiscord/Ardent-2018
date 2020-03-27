package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.commands.Precondition
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import com.ardentbot.kotlin.removeIndices
import com.ardentbot.kotlin.without
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("ardent")
class Admr : Command("admr", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments[0] == "v") {
            register.newVersionInfo = arguments.without(0).concat()
        }
    }

    val developer = Precondition({ it.event.author.id == "169904324980244480" }, {
        listOf(Emojis.HEAVY_MULTIPLICATION_X.cmd + it.translate("precondition.bot_developer"))
    })
}