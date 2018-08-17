package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.commands.music.getInfo
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Playing : Command("playing", arrayOf("np"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val manager = event.guild.getAudioManager(event.channel, register)
        val player = manager.player
        if (player.playingTrack == null) event.channel.send(translate("music.no_playing", event, register), register)
        else {
            val track = manager.manager.current!!
            event.channel.send(track.getInfo(event.guild, register, true), register)
            if (manager.player.isPaused) event.channel.send(Emojis.INFORMATION_SOURCE.cmd +
                    translate("music.currently_paused", event, register), register)
        }

    }
}