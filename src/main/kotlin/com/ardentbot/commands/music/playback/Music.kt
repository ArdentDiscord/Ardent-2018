package com.ardentbot.commands.music.playback

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.MockCommand
import com.ardentbot.core.commands.ModuleMapping
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
@MockCommand("play music on your server")
class Music : Command("music", arrayOf("mus"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        register.holder.getCommand("help")!!.onInvoke(event, arguments, listOf(Flag("m", "\"music\"")), register)
    }
}