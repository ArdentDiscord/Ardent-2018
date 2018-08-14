package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class UserLog : Command("ulog", arrayOf("userlog"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        getUser(arguments.concat(), event, this, register) { user ->
            if (user == null) register.sender.cmdSend(Emojis.CROSS_MARK.cmd +
                    translate("general.specify_or_mention_user", event, register), this, event)
            else {
                register.sender.cmdSend(translate("ulog.generating", event, register), this, event)
                val mentioned = event.guild.getMember(user)
                val mutualChannels = event.guild.textChannels.filter {
                    event.member.hasPermission(it, Permission.MESSAGE_READ) && mentioned.hasPermission(it, Permission.MESSAGE_READ)
                }.map { it.id }

                val messages = register.database.getMessagesFor(mentioned.user, event.guild)
                        .filter { mutualChannels.contains(it.channelId) }
                        .sortedByDescending { it.time }
                        .map {
                            it.time.localeDate() + " in #" + (it.channelId.toChannel(event.guild)?.name
                                    ?: "unknown channel") + ": " + it.content
                        }

                val url = paste("All cached messages for []".apply(mentioned.user.display()) + "\n-----------------\n" + messages.joinToString("\n"))
                event.author.openPrivateChannel().queue {
                    it.sendMessage(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            translate("ulog.generated_dump", event, register).apply(url) + "\n" +
                            Emojis.WARNING_SIGN.cmd + translate("ulog.warning", event, register)
                    ).queue()
                }
            }
        }
    }
}