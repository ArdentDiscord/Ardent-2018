package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.translation.Language
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class SetLanguage : Command("lang", arrayOf("language", "setlang"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) {
            event.channel.send("The current language is set to []. To change this, use */lang language_here*." + "\n" +
                    "**Language list**:" + Language.values().map { "${it.readable} (${it.id})" },register)
        }
    }
}