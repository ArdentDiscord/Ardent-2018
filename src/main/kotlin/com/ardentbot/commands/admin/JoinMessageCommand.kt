package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class JoinMessageCommand : Command("joinmessage", arrayOf("jm"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("params", event.guild, register) == true -> {
                val params = listOf(
                        Pair("user", translate("messages.user_param", event, register)),
                        Pair("server", translate("messages.server_param", event, register)),
                        Pair("serversize", translate("messages.server_size", event, register))
                )
                val embed = getEmbed(translate("joinmessage.params_title", event, register), event.author, event.guild)
                        .appendDescription(translate("messages.params_explanation", event, register)
                                .apply(translate("joinmessage.jm", event, register)) + "\n")
                        .appendDescription(
                                params.joinToString("\n") {
                                    Emojis.SMALL_BLUE_DIAMOND.cmd + "**[${it.first}]**" + " " + it.second
                                }
                        )
                register.sender.cmdSend(embed, this, event)
            }
            arg?.isTranslatedArgument("view", event.guild, register) == true -> {
                val data = register.database.getGuildData(event.guild)
                val embed = getEmbed(translate("joinmessage.view_title", event, register), event.author, event.guild)
                        .appendDescription(translate("messages.message_heading", event, register) + " " +
                                (data.joinMessage.message ?: translate("messages.no_message", event, register)))
                        .appendDescription("\n")
                        .appendDescription(translate("messages.where_send", event, register) + " " +
                                (data.joinMessage.channelId?.toChannel(event.guild)?.asMention
                                        ?: translate("messages.no_channel", event, register)))
                        .appendDescription("\n\n")
                        .appendDescription(translate("messages.how_change", event, register).apply("/jm"))
                register.sender.cmdSend(embed, this, event)
            }
            arg?.isTranslatedArgument("set", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd
                            + translate("messages.add_message", event, register), this, event)
                    else {
                        val data = register.database.getGuildData(event.guild)
                        val old = data.joinMessage.message
                        data.joinMessage.message = arguments.without(0).concat()
                        register.database.update(data)
                        event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                translate("general.update", event, register).apply(translate("joinmessage.jm", event, register),
                                        data.joinMessage.message!!, old
                                        ?: translate("general.none", event, register)), register)
                        if (data.joinMessage.channelId == null) {
                            register.sender.cmdSend(Emojis.WARNING_SIGN.cmd +
                                    translate("messages.setup_warning", event, register), this, event)
                        }
                    }
                }
            }
            arg?.isTranslatedArgument("channel", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd
                            + translate("messages.add_message", event, register), this, event)
                    else {
                        val channel = event.message.mentionedChannels.getOrNull(0)
                                ?: event.guild.getTextChannelsByName(arguments.without(0).concat(), true).getOrNull(0)
                        if (channel == null) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                translate("general.specify_channel", event, register), this, event)
                        else {
                            val data = register.database.getGuildData(event.guild)
                            val old = data.joinMessage.channelId?.let { event.guild.getTextChannelById(it) }
                            data.joinMessage.channelId = channel.id
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    translate("general.update", event, register).apply(translate("joinmessage.channel", event, register),
                                            channel.name, old?.name
                                            ?: translate("general.none", event, register)), this, event)
                        }
                    }
                }
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val data = register.database.getGuildData(event.guild)
                    when {
                        arguments.getOrNull(1)?.isTranslatedPhrase("general.message", event.guild, register) == true -> {
                            val old = data.joinMessage.message
                            data.joinMessage.message = null
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    translate("general.update", event, register).apply(translate("joinmessage.jm", event, register),
                                            translate("general.none", event, register), old
                                            ?: translate("general.none", event, register)), this, event)
                        }
                        arguments.getOrNull(1)?.isTranslatedArgument("channel", event.guild, register) == true -> {
                            val old = data.joinMessage.channelId?.let { event.guild.getTextChannelById(it) }
                            data.joinMessage.channelId = null
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    translate("general.update", event, register).apply(translate("joinmessage.channel", event, register),
                                            translate("general.none", event, register), old?.name
                                            ?: translate("general.none", event, register)), this, event)
                        }
                        else -> displayHelp(event, arguments, flags, register)
                    }
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val params = Argument("params")
    val set = Argument("set")
    val channel = Argument("channel")
    val view = Argument("view")
    val removeChannel = Argument("removechannel")
    val remove = Argument("remove")
}