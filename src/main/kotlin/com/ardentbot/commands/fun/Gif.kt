package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.commands.Command
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ModuleMapping
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.json.JSONObject
import org.jsoup.Jsoup

@ModuleMapping("fun")
class Gif : Command("gif", arrayOf("meme", "jif"), 5) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        register.sender.cmdSend(JSONObject(Jsoup.connect("https://api.giphy.com/v1/gifs/random").data("api_key", register.config["giphy"])
                .ignoreContentType(true).get().body().text()).getJSONObject("data").getString("image_url"), this, event)
    }
}