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
class ClearQueue : Command("clearqueue", arrayOf("clearq", "cq"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.member.hasPermission(event.channel, register, true) && event.member.checkSameChannel(event.channel, register)) {
            val audioManager = event.guild.getAudioManager(event.channel, register)
            audioManager.scheduler.autoplay = false
            audioManager.manager.resetQueue()
            event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.symbol + " " + "Successfully cleared the queue", register)
        }
    }
}