package com.ardentbot.commands.rpg

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.Marriage
import com.ardentbot.core.selectFromList
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.entities.impl.EmoteImpl
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class Marry : Command("marry", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val marriage = register.database.getMarriageFor(event.author.id)
        val mentioned = event.message.mentionedUsers.getOrNull(0)
        if (mentioned == null) {
            marriage?.let {
                register.sender.cmdSend("You're married to **[]**!"
                        .apply(register.getUser(if (marriage.first == event.author.id) marriage.second else marriage.first)?.display()
                                ?: "unknown") + "\n" + "You can divorce them with */divorce*, but be aware: if you initiate a divorce, " +
                        "**50%** of your money may go to the divorcee", this, event)
            } ?: {
                register.sender.cmdSend("**" + "You're not married!" + "**\n" + "You can marry someone with */marry @User*!", this, event)
            }.invoke()
        } else if (mentioned.isBot) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You can't marry a bot, but nice try! As for me, I'll forever love Adam.", this, event)
        } else if (marriage != null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Polygamy isn't allowed >.>", this, event)
        } else if (register.database.getMarriageFor(mentioned.id) != null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "**[]** is already married!".apply(mentioned.display()), this, event)
        } else if (mentioned.id == event.author.id) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Just.. why?", this, event)
        } else {
            register.sender.cmdSend("[], [] is now proposing to you!".apply(mentioned.asMention, event.author.asMention), this, event)
            event.channel.selectFromList(event.guild.getMember(mentioned), "Do you want to accept this proposal?", listOf("yes", "no"),
                    { i, _ ->
                        if (i == 0) {
                            if(register.database.getMarriageFor(mentioned.id) != null || register.database.getMarriageFor(event.author.id) != null) {
                                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Illegal state.. is someone now married?", this, event)
                            } else {
                                register.database.insert(Marriage(event.author.id, mentioned.id, System.currentTimeMillis()))
                                register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Congrats! You're now married", this, event)
                            }
                        } else register.sender.cmdSend("Ouch.. [] rejected you".apply(mentioned.asMention), this, event)
                    }, register = register
            )
        }
    }
}