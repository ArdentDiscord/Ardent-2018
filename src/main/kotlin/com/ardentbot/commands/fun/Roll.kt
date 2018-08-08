package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.FlagModel
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.get
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.diamond
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("fun")
class Roll : Command("roll", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (flags.isEmpty()) displayHelp(event, arguments, flags, register)
        else {
            val sidesFlag = flags.get("s")?.value?.toIntOrNull() ?: 6
            val numberFlag = flags.get("n")?.value?.toIntOrNull() ?: 1
            val default = flags.get("d")

            var number = 1
            var sides = 6
            if (default == null) {
                if (sidesFlag <= 1) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Please input a valid side amount!", this, event)
                else sides = sidesFlag
                if (numberFlag <= 1 || numberFlag > 10) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Please input a valid number of rolls (1 to 10)!", this, event)
                else number = numberFlag
            }

            if (number == 1) {
                register.sender.cmdSend(":game_die Rolling IRL . . .", this, event, callback = {
                    it.editMessage("Rolled a []-sided die. Result: **[]**".apply(sides, register.random.nextInt(6) + 1)).queue()
                })
            } else {
                val embed = getEmbed("Roll | []-sided die".apply(sides), event.author, event.guild)
                repeat(number) { embed.appendDescription(it.diamond() + "**${it + 1}**: " + (register.random.nextInt(sides) + 1) + "\n") }
                register.sender.cmdSend(embed, this, event)
            }
        }
    }

    val sides = FlagModel("s", "sides")
    val number = FlagModel("n", "number")
    val default = FlagModel("d", "default")

    val example = "-d (roll a 6-sided die once)"
    val example2 = "-s 4 -n 2 (roll a 4-sided die 2 times)"
}