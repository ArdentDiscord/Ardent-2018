package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.FlagModel
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.get
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("fun")
class Random : Command("random", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when {
            flags.get("n") != null -> {
                val number = flags.get("n")!!.value?.toIntOrNull()
                when {
                    number == null -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("random.specify_valid_number", event, register), this, event)
                    number <= 0 -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("general.need_positive_number", event, register), this, event)
                    else -> register.sender.cmdSend(translate("random.chosen_number", event, register).apply(register.random.nextInt(number) + 1), this, event)
                }
            }
            flags.get("options") != null -> {
                val options = flags.get("options")!!.values
                if (options == null || options.isEmpty()) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        translate("random.specify_valid_options",event, register), this, event)
                else register.sender.cmdSend(translate("random.chosen_option",event, register).apply(options[register.random.nextInt(options.size)]), this, event)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val num = FlagModel("n", "number")
    val options = FlagModel("options", "options")

    val example = "-options \"one\" \"two\" \"three\""
}