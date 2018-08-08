package com.ardentbot.commands.ardent

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("ardent")
class About : Command("2018", arrayOf("about"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val embed = getEmbed("Ardent | About", event.author, event.guild)
                .appendDescription(""""Ardent is in its best iteration yet, with the same games and features as previous iterations.
                    |With music coming soon, you may wonder what's different about this version, and why you should use it.""" + "\n\n" +
                        """Ardent is more stable and innovative than ever. Our custom UNIX-style parser, command system, and
                            |website allow us to make Ardent even faster, even with increased integration with 3rd party systems.
                            |
                            |Though Ardent is only in beta, we think you'll like what you see. Have fun with Ardent!
                        """.trimMargin())
        register.sender.cmdSend(embed, this, event)
    }
}