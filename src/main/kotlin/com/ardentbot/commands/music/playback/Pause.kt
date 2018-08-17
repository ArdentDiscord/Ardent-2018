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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Pause : Command("pause", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (!event.member.checkSameChannel(event.channel, register) || !event.member.hasPermission(event.channel, register, musicCommand = true)) return
        val audioManager = event.guild.getAudioManager(event.channel, register)
        when {
            audioManager.player.playingTrack == null -> event.channel.send(translate("music.no_playing",event, register), register)
            audioManager.player.isPaused -> event.channel.send(translate("pause.already_paused",event, register), register)
            else -> {
                audioManager.player.isPaused = true
                event.channel.send(translate("pause.response",event, register) + " ${Emojis.WHITE_HEAVY_CHECKMARK}", register)
            }
        }
    }
}