package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.FlagInformation
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.get
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException

@ModuleMapping("admin")
class Clear : Command("clear", arrayOf("cl"), 4) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (flags.isEmpty() && arguments.getOrNull(0)?.toIntOrNull() == null) {
            displayHelp(event, arguments, flags, register)
            return
        }
        val amount = flags.get("n")?.value?.toIntOrNull() ?: arguments.getOrNull(0)?.toIntOrNull() ?: 10
        if (amount !in 2..100) register.sender.cmdSend("You specified an invalid number of messages to clear. Must be " +
                "between 2 and 100!", this, event)
        else {
            val channel = flags.get("c")?.value?.toChannelId()?.toChannel(event.guild) ?: event.channel
            val user = flags.get("u")?.value?.toUserId()?.toMember(event.guild)

            var toDelete = 0
            val history = channel.iterableHistory
            val messages = history.takeWhile { message ->
                if ((message.editedTime ?: message.creationTime).plusWeeks(2).toInstant()
                                .toEpochMilli() < System.currentTimeMillis()) false
                else {
                    if (amount + 1 > toDelete && (if (user != null) message.author.id == user.user.id else true)) {
                        toDelete++
                        true
                    } else false
                }
            }
            event.message.delete().queue()
            if (messages.isEmpty()) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "No message matching your specification was found", register)
            else {
                (if (messages.size == 1) messages[0].delete() else channel.deleteMessages(messages.without(0))).queue({
                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Cleared [] messages in []".apply(messages.size - 1, channel.asMention), this, event)
                }, { e ->
                    if (e is InsufficientPermissionException) {
                        register.sender.cmdSend("I can't remove those messages. Please make sure I have the `Message Manage` permission!", this, event)
                        return@queue
                    } else e.printStackTrace()
                })
            }
        }
    }

    val example = "-u @Adam (clears 10 messages from Adam)"
    val example2 = "-c #general -n 4 (clears the last 4 messages in #general)"

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MESSAGE_MANAGE))

    val user = FlagInformation("u", "@User", "clear messages from a specific user")
    val quantity = FlagInformation("n", "# of messages to delete (2-100)", "specify an amount of " +
            "messages to clear (default 10)")
    val channel = FlagInformation("c", "#channel", "clear messages in a specific channel")
    val default = FlagInformation("d", null, "clear the last 10 messages in the current channel")
}