package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class UserLog : Command("ulog",
        null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (event.message.mentionedUsers.isEmpty()) register.sender.cmdSend(Emojis.CROSS_MARK.cmd + "You need to mention a user!", this, event)
        else {
            register.sender.cmdSend("Generating the user log.. check your DMs in a few seconds", this, event)
            val mentioned = event.guild.getMember(event.message.mentionedUsers[0])
            val mutualChannels = event.guild.textChannels.filter {
                event.member.hasPermission(it, Permission.MESSAGE_READ) && mentioned.hasPermission(it, Permission.MESSAGE_READ)
            }.map { it.id }
            val messages = register.database.getMessagesFor(mentioned.user, event.guild)
                    .filter { mutualChannels.contains(it.channelId) }
                    .sortedByDescending { it.time }
                    .map {
                        it.time.localeDate() + " in #" + (it.channelId.toChannel(event.guild)?.name
                                ?: "UNKNOWN_CHANNEL") + ": " + it.content
                    }
            val url = paste("All cached messages for []".apply(mentioned.user.display()) + "\n-----------------\n" + messages.joinToString("\n"))
            event.author.openPrivateChannel().queue {
                it.sendMessage(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Generated a dump of all found messages at []"
                        .apply(url)).queue()
            }
        }
    }
}