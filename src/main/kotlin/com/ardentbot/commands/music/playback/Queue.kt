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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Queue : Command("queue", arrayOf("q"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed("Current Music Queue",event.author,event.guild)
        val audioManager = event.guild.getAudioManager(event.channel,register)
        if (audioManager.manager.current == null) {
            embed.appendDescription(Emojis.INFORMATION_SOURCE.symbol + " " + "There aren't any currently playing tracks!")
            embed.appendDescription("\n\n" + "You can view the queue online by clicking [here]([])".apply("https://ardentbot.com/music/queue/${event.guild.id}"))
        } else {
            if (audioManager.manager.queue.size == 0) {
                embed.appendDescription("There are no songs in the queue!")
            }
            else {
                var current = 1
                audioManager.manager.queue.stream().limit(10).forEachOrdered {
                    embed.appendDescription("#$current: " + it.getInfo(event.guild,register) + "\n")
                    current++
                }
            }
            embed.appendDescription("\n\n" + "You can view the queue online by clicking [here]([])".apply("https://ardentbot.com/music/queue/${event.guild.id}"))
        }
        event.channel.send(embed,register)
    }
}