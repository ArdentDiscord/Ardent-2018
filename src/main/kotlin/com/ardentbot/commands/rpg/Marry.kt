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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class Marry : Command("marry", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val marriage = register.database.getMarriageFor(event.author.id)
        val mentioned = event.message.mentionedUsers.getOrNull(0)
        if (mentioned == null) {
            marriage?.let {
                register.sender.cmdSend(translate("marry.current", event, register)
                        .apply(register.getUser(if (marriage.first == event.author.id) marriage.second else marriage.first)?.display()
                                ?: translate("unknown", event, register)+"\n")
                        + translate("marry.warning", event, register), this, event)
            } ?: {
                register.sender.cmdSend("**" + translate("marriage.not_married", event, register) + "**\n" +
                        translate("marry.how_to", event, register), this, event)
            }.invoke()
        } else if (mentioned.isBot) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("marry.no_marry_bot", event, register), this, event)
        } else if (marriage != null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("marry.no_polygamy", event, register) + " >.>", this, event)
        } else if (register.database.getMarriageFor(mentioned.id) != null) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("marry.already_married", event, register).apply(mentioned.display()), this, event)
        } else if (mentioned.id == event.author.id) {
            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("marry.just_why", event, register), this, event)
        } else {
            register.sender.cmdSend(translate("marry.proposal_message", event, register).apply(mentioned.asMention, event.author.asMention), this, event)
            event.channel.selectFromList(event.guild.getMember(mentioned), translate("marry.accept", event, register),
                    listOf(translate("yes", event, register), translate("no", event, register)),
                    { i, _ ->
                        if (i == 0) {
                            if (register.database.getMarriageFor(mentioned.id) != null || register.database.getMarriageFor(event.author.id) != null) {
                                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("marry.illegal_state", event, register), this, event)
                            } else {
                                register.database.insert(Marriage(event.author.id, mentioned.id, System.currentTimeMillis()))
                                register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + translate("marry.now_married", event, register), this, event)
                            }
                        } else register.sender.cmdSend(translate("marry.rejected", event, register).apply(mentioned.asMention), this, event)
                    }, register = register
            )
        }
    }
}