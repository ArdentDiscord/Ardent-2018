package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class JoinMessageCommand : Command("joinmessage", arrayOf("jm"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "params" -> {
                val params = listOf(
                        Pair("user", "mentions the user who has just joined"),
                        Pair("server", "replaced with the name of the server"),
                        Pair("serversize", "replaced with the amount of users in this server")
                )
                val embed = getEmbed("Parameters | Join Message", event.author, event.guild)
                        .appendDescription("""In the join message, you can use the following parameters to give
                            |your server a more customized appearance.""".trimMargin() + "\n")
                        .appendDescription(
                                params.joinToString("\n") {
                                    Emojis.SMALL_BLUE_DIAMOND.cmd + "**[${it.first}]**" + " " + it.second
                                }
                        )
                register.sender.cmdSend(embed, this, event)
            }
            "view" -> {
                val data = register.database.getGuildData(event.guild)
                val embed = getEmbed("Join Message | Ardent", event.author, event.guild)
                        .appendDescription("**Message**: " + (data.joinMessage.message
                                ?: "No message has been set up"))
                        .appendDescription("\n")
                        .appendDescription("**Channel** to send message to: " +
                                (data.joinMessage.channelId?.toChannel(event.guild)?.asMention
                                        ?: "No channel has been set up"))
                        .appendDescription("\n\n")
                        .appendDescription("You can change these settings. See how with /joinmessage")
                register.sender.cmdSend(embed, this, event)
            }
            "set" -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd
                            + "You need to add a message", this, event)
                    else {
                        val data = register.database.getGuildData(event.guild)
                        val old = data.joinMessage.message
                        data.joinMessage.message = arguments.without(0).concat()
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                "You set the join message to: **[]**\nOld message: **[]**"
                                        .apply(data.joinMessage.message!!, old ?: "None"), this, event)
                        if (data.joinMessage.channelId == null) {
                            register.sender.cmdSend(Emojis.WARNING_SIGN.cmd +
                                    "Warning! You've set a join message, but not specified a channel to send it to. Without one set up, your automessage won't work", this, event)
                        }
                    }
                }
            }
            "channel" -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd
                            + "You need to add a message", this, event)
                    else {
                        val channel = event.message.mentionedChannels.getOrNull(0)
                                ?: event.guild.getTextChannelsByName(arguments.without(0).concat(), true).getOrNull(0)
                        if (channel == null) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                "You need to specify a valid channel name or mention", this, event)
                        else {
                            val data = register.database.getGuildData(event.guild)
                            val old = data.joinMessage.channelId?.let { event.guild.getTextChannelById(it)}
                            data.joinMessage.channelId = channel.id
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    "You set the join message channel to: **[]**\nOld channel: **[]**"
                                            .apply(channel.name, old?.name ?: "None"), this, event)
                        }
                    }
                }
            }
            "remove" -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val data = register.database.getGuildData(event.guild)
                    when (arguments.getOrNull(1)) {
                        "message" -> {
                            val old = data.joinMessage.message
                            data.joinMessage.message = null
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    "You set the join message to: **[]**\nOld message: **[]**"
                                            .apply("None", old ?: "None"), this, event)
                        }
                        "channel" -> {
                            val old = data.joinMessage.channelId?.let { event.guild.getTextChannelById(it) }
                            data.joinMessage.channelId = null
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    "You set the join message channel to: **[]**\nOld channel: **[]**"
                                            .apply("None", old?.name ?: "None"), this, event)
                        }
                        else -> displayHelp(event, arguments, flags, register)
                    }
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val params = ArgumentInformation("params", "see what special parameters you can use in your join message!")
    val set = ArgumentInformation("set [message]", "set the join message.")
    val channel = ArgumentInformation("channel [mentioned channel or name]", "set the channel where you want ")
    val view = ArgumentInformation("view", "view the current message parameters")
    val removeChannel = ArgumentInformation("remove channel", "remove the set join message channel")
    val remove = ArgumentInformation("remove message", "remove the set join message")
}