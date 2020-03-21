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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Volume : Command("volume", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val audioManager = event.guild.getAudioManager(event.channel, register)
        if (arguments.isEmpty()) event.channel.send(Emojis.PUBLIC_ADDRESS_LOUDSPEAKER.cmd +
                translate("volume.info", event, register).apply(audioManager.player.volume), register)
        else {
            if (event.member!!.hasPermission(event.channel, register, true) && event.member!!.checkSameChannel(event.channel, register)) {
                val volume = arguments[0].replace("%", "").toIntOrNull()
                if (volume == null || volume < 0 || volume > 100) {
                    event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("general.valid_percentage", event, register), register)
                } else {
                    audioManager.player.volume = volume
                    event.channel.send(Emojis.PUBLIC_ADDRESS_LOUDSPEAKER.cmd + translate("volume.response", event, register).apply(volume), register)
                }
            }
        }

    }
}