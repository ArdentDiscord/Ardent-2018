package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.load
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.concat
import com.ardentbot.kotlin.without
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Play : Command("play", arrayOf("p"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) {
            event.channel.send(Emojis.INFORMATION_SOURCE.cmd + translate("music.status_playing", event, register) + "\n" +
                    translate("music.how_search", event, register) + "\n" +
                    "\n" + translate("music.how_lucky", event, register) + "\n\n" +
                    translate("music.how_mymusic", event, register), register)
        } else {
            if (arguments[0].equals("lucky", true)) arguments.without(0).concat().load(event.member!!, event.channel, register, lucky = true)
            else arguments.concat().load(event.member!!, event.channel, register)
        }
    }
}