package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.checkSameChannel
import com.ardentbot.core.database.hasPermission
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Goto : Command("goto", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) event.channel.send(translate("goto.how", event, register), register)
        else {
            val split = arguments[0].split(":")
            val minutes = split.getOrNull(0)?.toIntOrNull()
            val seconds = split.getOrNull(1)?.toIntOrNull()
            if (split.size != 2 || minutes == null || seconds == null) event.channel.send(translate("goto.how",event, register), register)
            else {
                val audioManager = event.guild.getAudioManager(event.channel, register)
                if (audioManager.manager.current == null) event.channel.send(translate("music.no_playing",event, register), register)
                else if (event.member.hasPermission(event.channel, register, true) && event.member.checkSameChannel(event.channel, register)) {
                    if (minutes * 60 + seconds < 0 || minutes * 60 + seconds > audioManager.player.playingTrack.duration / 1000) {
                        event.channel.send(translate("goto.invalid_position",event, register), register)
                    } else {
                        audioManager.player.playingTrack.position = ((minutes * 60 + seconds) * 1000).toLong()
                        event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                translate("goto.response",event, register).apply(arguments[0]), register)
                    }
                }
            }
        }
    }
}