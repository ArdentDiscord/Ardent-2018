package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.exceptions.HierarchyException

@ModuleMapping("fun")
class Nickme : Command("nickme", arrayOf("nameme"), null) {
    val words = mutableListOf<Word>()
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (words.isEmpty()) {
            words.addAll(register.database.deserializeWebsite("https://randomwordgenerator.com/json/words.json", WordsResponse::class.java).data)
        }
        val word = if (arguments.isEmpty()) words[register.random.nextInt(words.size)].word.capitalize()
        else arguments.concat()

        try {
            event.guild.modifyNickname(event.member!!, word).reason("/nickme").queue {
                register.sender.cmdSend(translate("nickme.changed", event, register).apply(event.author.name, word), this, event)
            }
        } catch (e: Exception) {
            if (e is HierarchyException) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    translate("nickme.exception", event, register) +
                    " " + if (word != arguments.concat()) translate("nickme.however", event, register).apply(word) else "", this, event)
            else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("permission", event, register)
                    .apply(translate("nick.permission", event, register)), this, event)
        }
    }

}

data class WordsResponse(val data: List<Word>)
data class Word(val word: String)