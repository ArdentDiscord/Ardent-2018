package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.checkSameChannel
import com.ardentbot.core.database.hasPermission
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class RemoveFrom : Command("removefrom", arrayOf("rf"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.member!!.hasPermission(event.channel, register, true) && event.member!!.checkSameChannel(event.channel, register)) {
            getUser(arguments.concat(), event, this, register) { user ->
                if (user == null) event.channel.send(translate("general.specify_or_mention_user", event, register), register)
                else {
                    val audioManager = event.guild.getAudioManager(event.channel, register)
                    val sizeBefore = audioManager.manager.queue.size
                    audioManager.manager.queue.removeIf { it.user == user.id }
                    event.channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + translate("rmf.response", event, register)
                            .apply(audioManager.manager.queue.size - sizeBefore, user.display()), register)
                }
            }
        }
    }
}