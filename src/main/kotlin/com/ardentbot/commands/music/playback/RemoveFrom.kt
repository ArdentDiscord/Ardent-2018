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
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class RemoveFrom : Command("removefrom", arrayOf("rf"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.member.hasPermission(event.channel, register, true) && event.member.checkSameChannel(event.channel, register)) {
            val mentioned = event.message.mentionedUsers
            if (mentioned.size == 0 || mentioned.size > 1) event.channel.send("You must mention one user!", register)
            else {
                val audioManager = event.guild.getAudioManager(event.channel, register)
                val sizeBefore = audioManager.manager.queue.size
                audioManager.manager.queue.removeIf { it.user == mentioned[0].id }
                event.channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + "Successfully removed **[]** queued tracks from **[]**"
                        .apply(audioManager.manager.queue.size - sizeBefore, mentioned[0].display()), register)
            }
        }
    }
}