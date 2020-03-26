package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.commands.music.getInfo
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.web.base
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Queue : Command("queue", arrayOf("q"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed("Current Music Queue", event.author, event.guild)
        val audioManager = event.guild.getAudioManager(event.channel, register)
        if (audioManager.manager.current == null) {
            embed.appendDescription(Emojis.INFORMATION_SOURCE.cmd + translate("music.no_playing", event, register))
            embed.appendDescription("\n\n" + translate("queue.view_online", event, register).apply("$base/music/queue/${event.guild.id}"))
        } else {
            if (audioManager.manager.queue.size == 0) {
                embed.appendDescription(translate("queue.no_songs_in_queue", event, register))
            } else {
                var current = 1
                audioManager.manager.queue.stream().limit(10).forEachOrdered {
                    embed.appendDescription("#$current: " + it.getInfo(event.guild, register) + "\n")
                    current++
                }
            }
            embed.appendDescription("\n\n" + translate("queue.view_online", event, register).apply("$base/music/queue/${event.guild.id}"))
        }
        event.channel.send(embed, register)
    }
}