package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.FlagInformation
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
                    number == null -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a valid number", this, event)
                    number <= 0 -> register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "The number must be positive!", this, event)
                    else -> register.sender.cmdSend("Your number is: **[]**".apply(register.random.nextInt(number) + 1), this, event)
                }
            }
            flags.get("options") != null -> {
                val options = flags.get("options")!!.values
                if (options == null || options.isEmpty()) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify at least 2 options " +
                        "in the format: */random options \"one\" \"two\"", this, event)
                else register.sender.cmdSend("Your chosen option is: **[]**".apply(options[register.random.nextInt(options.size)]), this, event)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val num = FlagInformation("n", "**number**", "select a random number between 1 and the specified number")
    val options = FlagInformation("options", "**quote-separated list of options**. e.g. /random -options \"one\" \"two\"",
            "select a random option given a list")

    val example = "-options \"one\" \"two\" \"three\""
}