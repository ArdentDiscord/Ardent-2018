package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.HierarchyException

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
            event.guild.controller.setNickname(event.member, word).reason("/nickme").queue {
                register.sender.cmdSend("[], your nickname is now **[]**".apply(event.author.name, word), this, event)
            }
        } catch (e: Exception) {
            if (e is HierarchyException) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    "Unable to set your nickname. Due to Discord limitations, I cannot set the nickname of the server owner or members with a higher role than myself. " +
                    if (word != arguments.concat()) "However, the generated name is **[]**".apply(word) else "", this, event)
            else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Unable to set your nickname.. do I have permission?", this, event)
        }
    }

}

data class WordsResponse(val data: List<Word>)
data class Word(val word: String)