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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Repeat : Command("repeat", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.member!!.checkSameChannel(event.channel, register) && event.member!!.hasPermission(event.channel, register, true)) {
            val audioManager = event.guild.getAudioManager(event.channel, register)
            if (audioManager.manager.current != null) {
                audioManager.manager.addToBeginningOfQueue(audioManager.manager.current!!)
                event.channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + translate("repeat.response", event, register), register)
            } else event.channel.send(translate("music.no_playing", event, register), register)
        }
    }
}