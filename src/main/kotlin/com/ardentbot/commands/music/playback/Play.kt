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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Play : Command("play", arrayOf("p"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) {
            event.channel.send(Emojis.INFORMATION_SOURCE.cmd + "Music info: **Playing**" + "\n" +
                    "You can search or play single tracks from Youtube or Spotify by typing */play [search or url]*." + "\n" +
                    "\n" + "Automatically play the first search result using */play lucky [search]*." +
                    "\n\nYou can also type */mymusic play* to play your personal music library or */playlist [playlist id] play* to play one of your playlists.", register)
        } else {
            if (arguments[0].equals("lucky",true)) arguments.without(0).concat().load(event.member,event.channel,register,lucky=true)
            else arguments.concat().load(event.member, event.channel, register)
        }
    }
}