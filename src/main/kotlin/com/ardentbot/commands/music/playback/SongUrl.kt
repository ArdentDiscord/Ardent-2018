package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.checkSameChannel
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class SongUrl : Command("songurl", arrayOf("songlink", "su", "sl"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (!event.member.checkSameChannel(event.channel, register)) return
        val player = event.guild.getAudioManager(event.channel, register).player
        if (player.playingTrack == null) event.channel.send(translate("music.no_playing", event, register), register)
        else event.channel.send(translate("songurl.response", event, register)
                .apply(player.playingTrack.info.title, player.playingTrack.info.author, player.playingTrack.info.uri), register)
    }
}