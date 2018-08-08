package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color

@ModuleMapping("fun")
class UrbanDictionary : Command("urban",
        arrayOf("urbandictionary", "ud"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) displayHelp(event, arguments, flags, register)
        else {
            val search = register.database.deserializeWebsite("http://api.urbandictionary.com/v0/define?term=${arguments.concat().encode()}",
                    UrbanDictionarySearch::class.java)
            if (search.list.isEmpty()) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                    "No results found for this search query", this, event)
            else {
                val result = search.list[0]
                val embed = getEmbed("Urban Dictionary Definition (first) for []".apply(result.word), event.author, event.guild)
                        .setThumbnail("https://i.gyazo.com/6a40e32928743e68e9006396ee7c2a14.jpg")
                        .setColor(Color.decode("#00B7BE"))
                        .addField("Definition", result.definition shortenTo 1024, true)
                        .addField("Example", result.example shortenTo 1024, true)
                        .addField("Thumbs Up " + Emojis.THUMBS_UP.symbol, result.thumbs_up.toString(), true)
                        .addField("Thumbs Down" + Emojis.THUMBS_DOWN.symbol, result.thumbs_down.toString(), true)
                        .addField("Author", result.author, true)
                        .addField("Permalink", result.permalink, true)
                register.sender.cmdSend(embed, this, event)
            }
        }
    }
}

data class UrbanDictionarySearch(val tags: List<String>, val result_type: String, val list: List<UrbanDictionaryResult>, val sounds: List<String>)

data class UrbanDictionaryResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                                 val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)