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
            audioManager.player.playingTrack == null -> event.channel.send("There isn't a playing track!", register)
            audioManager.player.isPaused -> event.channel.send("The player is already paused!", register)
            else -> {
                audioManager.player.isPaused = true
                event.channel.send("Paused playback" + " ${Emojis.WHITE_HEAVY_CHECKMARK}", register)
            }
        }
    }
}