package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.checkSameChannel
import com.ardentbot.core.database.hasPermission
import com.ardentbot.core.managers
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Stop : Command("stop", arrayOf("leave"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.guild.audioManager.isConnected) {
            if (!event.member!!.checkSameChannel(event.channel, register) || !event.member!!.hasPermission(event.channel, register, musicCommand = true)) return
            val audioManager = event.guild.getAudioManager(event.channel, register)
            audioManager.scheduler.autoplay = false
            audioManager.player.destroy()
            managers.remove(audioManager.guild.idLong)
            event.guild.audioManager.closeAudioConnection()
            audioManager.scheduler.autoplay = false
            event.channel.send(Emojis.HEAVY_CHECK_MARK.cmd + translate("stop.response", event, register), register)
        } else event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("music.not_in_voice", event, register), register)
    }
}