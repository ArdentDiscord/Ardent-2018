package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
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
                    translate("ud.404", event, register), this, event)
            else {
                val result = search.list[0]
                val embed = getEmbed(translate("ud.embed_title", event, register).apply(result.word), event.author, event.guild)
                        .setThumbnail("https://i.gyazo.com/6a40e32928743e68e9006396ee7c2a14.jpg")
                        .setColor(Color.decode("#00B7BE"))
                        .addField(translate("ud.definition", event, register), result.definition shortenTo 1024, true)
                        .addField(translate("cmd.example", event, register), result.example shortenTo 1024, true)
                        .addField(translate("ud.thumbs_up", event, register) + " " + Emojis.THUMBS_UP.symbol, result.thumbs_up.toString(), true)
                        .addField(translate("ud.thumbs_down", event, register) + " " + Emojis.THUMBS_DOWN.symbol, result.thumbs_down.toString(), true)
                        .addField(translate("ud.author", event, register), result.author, true)
                        .addField(translate("ud.permalink", event, register), result.permalink, true)
                register.sender.cmdSend(embed, this, event)
            }
        }
    }
}

data class UrbanDictionarySearch(val tags: List<String>, val result_type: String, val list: List<UrbanDictionaryResult>, val sounds: List<String>)

data class UrbanDictionaryResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                                 val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)