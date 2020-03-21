package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.commands.music.getCurrentTime
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.checkSameChannel
import com.ardentbot.core.database.hasPermission
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Skip : Command("skip", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (!event.member!!.checkSameChannel(event.channel, register) || !event.member!!.hasPermission(event.channel, register, true)) return
        val manager = event.guild.getAudioManager(event.channel, register)
        if (manager.player.playingTrack == null) return
        val track = manager.manager.current!!
        event.channel.send(translate("skip.response", event, register) + translate("music.play_info", event, register)
                .apply(track.track!!.info.title, track.track!!.info.author, track.track!!.getCurrentTime(), register.getUser(track.user)?.display()
                        ?: translate("unknown", event, register)), register)
        manager.scheduler.autoplay = false
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
    }
}