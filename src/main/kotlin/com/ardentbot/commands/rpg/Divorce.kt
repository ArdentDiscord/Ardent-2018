package com.ardentbot.commands.rpg

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class Divorce : Command("divorce", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val marriage = register.database.getMarriageFor(event.author.id)
        if (marriage == null) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You're not married!", this, event)
        else {
            register.sender.cmdSend("Are you sure you want to divorce this person? There's a 50% chance that half of your assets will be " +
                    "transferred to them. Respond **yes** if you want to go through with the divorce, or **no** if not.", this, event)
            Sender.waitForMessage({ it.channel.id == event.channel.id && it.author.id == event.author.id && it.guild.id == event.guild.id },
                    {
                        if (it.message.contentRaw.startsWith("y", true)) {
                            register.database.delete(marriage)
                            register.sender.cmdSend("You're now single.", this, event)
                            val other = register.getUser((if (marriage.first == event.author.id) marriage.second else marriage.first))
                            if (other != null && register.random.nextBoolean()) {
                                val divorceeData = register.database.getUserData(other.id)
                                val data = register.database.getUserData(event.author.id)

                                divorceeData.money += data.money / 2
                                data.money /= 2

                                register.database.update(divorceeData)
                                register.database.update(data)

                                register.sender.cmdSend("Unlucky! Half of your assets were transferred to your ex.", this, event)
                            }
                        } else register.sender.cmdSend("Canceling..", this, event)
                    })
        }
    }
}