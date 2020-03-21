package com.ardentbot.commands.rpg

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class Divorce : Command("divorce", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val marriage = register.database.getMarriageFor(event.author.id)
        if (marriage == null) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                translate("marriage.not_married", event, register), this, event)
        else {
            register.sender.cmdSend(translate("divorce.confirm", event, register), this, event)
            Sender.waitForMessage({ it.channel.id == event.channel.id && it.author.id == event.author.id && it.guild.id == event.guild.id },
                    {
                        if (it.message.contentRaw.startsWith("y", true) || it.message.contentRaw.isTranslatedPhrase("yes", event.guild, register)) {
                            register.database.delete(marriage)
                            register.sender.cmdSend(translate("divorce.now_single", event, register), this, event)
                            val other = register.getUser((if (marriage.first == event.author.id) marriage.second else marriage.first))
                            if (other != null && register.random.nextBoolean()) {
                                val divorceeData = register.database.getUserData(other.id)
                                val data = register.database.getUserData(event.author.id)

                                divorceeData.money += data.money / 2
                                data.money /= 2

                                register.database.update(divorceeData)
                                register.database.update(data)

                                register.sender.cmdSend(translate("divorce.asset_transfer", event, register), this, event)
                            }
                        } else register.sender.cmdSend(translate("canceling", event, register), this, event)
                    })
        }
    }
}