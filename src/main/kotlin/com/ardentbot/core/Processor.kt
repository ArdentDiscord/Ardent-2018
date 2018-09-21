package com.ardentbot.core

import com.ardentbot.commands.games.send
import com.ardentbot.core.database.UserCommand
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.events.user.update.UserUpdateOnlineStatusEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import org.apache.commons.lang3.exception.ExceptionUtils

class Processor(val register: ArdentRegister) {
    var receivedMessages = 0
    var receivedCommands = 0
    @SubscribeEvent
    fun process(event: Event) {
        Sender.check(event)
        when (event) {
            is GuildMessageReceivedEvent -> {
                receivedMessages++
                if (event.author.isBot || event.isWebhookMessage) return
                val data = register.database.getGuildData(event.guild)
                val language = data.language ?: Language.ENGLISH
                val prefixes = data.prefixesModified(register).map { it.prefix }
                val commandName = register.parser.parseBase(event.message, prefixes) ?: return
                register.holder.commands.firstOrNull {
                    it.getTranslatedName(event.guild, register) == commandName
                            || it.getTranslatedEnglishName(register) == commandName || it.aliases?.contains(commandName) == true
                }
                        ?.let { command ->
                            register.database.insert(UserCommand(event.author.id, event.channel.id, event.guild.id, command.name), blocking = false)
                            if (data.disabledModules.map { it.name }.contains(register.holder.getModuleFor(command).id)) {
                                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                        register.translationManager.translate("general.module_disabled", language)
                                                .apply(register.holder.getModuleFor(command).name), command, event)
                                return
                            }
                            if (data.disabledCommands.map { it.name }.contains(command.name)) {
                                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                        register.translationManager.translate("general.command_disabled", language).apply(command.name), command, event)
                                return
                            }
                            val prefix = register.parser.lookupPrefix(event.message, prefixes)
                            val parsedMessage = register.parser.parseMessage(event.message, prefix, commandName)
                                    ?: return
                            register.cachedExecutor.execute {
                                try {
                                    receivedCommands++
                                    command.check(event, parsedMessage.arguments, parsedMessage.flags, register)
                                } catch (e: Exception) {
                                    event.channel.send(register.translationManager.translate("general.error_message", language)
                                            .apply("**${e.localizedMessage}**") + "\n\n" +
                                            register.translationManager.translate("general.error_redirect", language)
                                                    .apply("https://discord.gg/Dtg23A7"), register)
                                    register.getTextChannel(register.config["error_channel"])!!.sendMessage(
                                            "**Time:** ${System.currentTimeMillis()}\n" +
                                                    "**User/Channel/Guild:** by ${event.author.display()} " +
                                                    "(${event.author.id}) in *${event.channel.name}* in server " +
                                                    "**${event.guild.name}**\n" +
                                                    "**Command:** ${command.name}\n" +
                                                    "**Message:** ${e.localizedMessage}"
                                    ).queue()
                                    register.getTextChannel(register.config["error_channel"])!!.sendMessage("^\n" +
                                            ExceptionUtils.getStackTrace(e)).queue()
                                }
                            }
                        }
            }
            is GuildMemberJoinEvent -> EventMessageSender.joinMessage(event, register)
            is GuildMemberLeaveEvent -> EventMessageSender.leaveMessage(event, register)
            is UserUpdateOnlineStatusEvent -> StatusUpdateChanger.change(event, register)
            is PrivateMessageReceivedEvent -> event.channel.sendMessage("Unfortunately, I don't support commands in private channels " +
                    "right now. Please retry in a server").queue()
            is GuildJoinEvent -> {
                try {
                    Thread.sleep(2500)
                    getSendChannel(event.guild).sendMessage("""Welcome to **Ardent**!
If this is your first time using Ardent, you may want to type **/help** to see what commands are available.
If you run into any issues, join our support server at <https://ardentbot.com/support> and we'd love to help you!

~Adam

*p.s: we suggest __/play__!*
                """.trimIndent()).queue()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

fun getSendChannel(guild: Guild): TextChannel {
    return guild.textChannels.asSequence().map {
        try {
            it to it.getMessageById(it.latestMessageId).complete().creationTime.toEpochSecond()
        } catch (e: Exception) {
            null
        }
    }.filterNotNull().sortedByDescending { it.second }.toList().getOrNull(0)?.first ?: guild.textChannels[0]
}