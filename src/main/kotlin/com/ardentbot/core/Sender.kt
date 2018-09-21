package com.ardentbot.core

import com.ardentbot.commands.admin.appendedAnnouncements
import com.ardentbot.core.commands.Command
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Sender(val register: ArdentRegister) {
    fun send(message: Any, command: Command?, channel: MessageChannel, user: User,
             event: GuildMessageReceivedEvent?, guild: Guild? = event?.guild,
             parameters: List<String>? = null, callback: ((Message) -> Unit)? = null) {
        val language = (guild ?: (channel as? TextChannel)?.guild)
                ?.let { g -> register.database.getGuildData(g).language }
                ?: Language.ENGLISH
        appendedAnnouncements.forEach {
            if (channel is TextChannel && !it.sentTo.contains(channel.guild.id)) {
                channel.sendMessage(Emojis.INFORMATION_SOURCE.cmd + register.translationManager.translate("sender.announcement", language) + " " + it.sentTo).queue()
                it.sentTo.add(channel.guild.id)
            }
        }
        try {
            val action = if (message is EmbedBuilder) channel.sendMessage(message.build())
            else channel.sendMessage(message.toString().replace("@here", "@ here")
                    .replace("@everyone", "@ everyone").apply(parameters ?: listOf()))

            action.queue({
                callback?.invoke(it)
            }, {
                user.openPrivateChannel()
                        .queue { channel ->
                            channel.sendMessage(
                                    register.translationManager.translate("error.message_send", language)
                                            .apply(if (message is EmbedBuilder) register.translationManager.translate("error.an_embed", language)
                                            else register.translationManager.translate("error.a_message", language),
                                                    event?.message?.contentRaw ?: command?.name ?: channel.name
                                                    ?: register.translationManager.translate("error.guild", language),
                                                    guild?.let { _ -> (channel as TextChannel).name }
                                                            ?: register.translationManager.translate("error.channel", language))
                            ).queue()
                        }
            })
        } catch (e: Exception) {
            if (e is InsufficientPermissionException) {
                user.openPrivateChannel().complete().sendMessage(register.translationManager.translate("error.cant_send_generic", language)
                        .apply("**${channel.name}**")).queue()
            } else e.printStackTrace()
        }
    }

    fun cmdSend(message: Any, command: Command, event: GuildMessageReceivedEvent, parameters: List<String>? = null,
                callback: ((Message) -> Unit)? = null) = send(message, command, event.channel, event.author, event, event.guild, parameters, callback)

    companion object {
        val messageReceivedEvents = ConcurrentLinkedQueue<WaiterSettings<GuildMessageReceivedEvent>>()
        val reactionAddedEvents = ConcurrentLinkedQueue<WaiterSettings<GuildMessageReactionAddEvent>>()
        val scheduledExecutor = Executors.newScheduledThreadPool(3)

        fun waitForMessage(condition: (GuildMessageReceivedEvent) -> Boolean, callback: (GuildMessageReceivedEvent) -> Unit, expiration: (() -> Unit)? = null,
                           time: Int = 20, timeUnit: TimeUnit = TimeUnit.SECONDS): WaiterSettings<GuildMessageReceivedEvent> {
            val settings = WaiterSettings(condition, callback, expiration)
            messageReceivedEvents.add(settings)
            scheduledExecutor.schedule({
                if (messageReceivedEvents.contains(settings)) {
                    expiration?.invoke()
                    messageReceivedEvents.remove(settings)
                }
            }, time.toLong(), timeUnit)
            return settings
        }

        fun waitForReaction(condition: (GuildMessageReactionAddEvent) -> Boolean, callback: (GuildMessageReactionAddEvent) -> Unit, expiration: (() -> Unit)? = null,
                            time: Int = 20, timeUnit: TimeUnit = TimeUnit.SECONDS): WaiterSettings<GuildMessageReactionAddEvent> {
            val settings = WaiterSettings(condition, callback, expiration)
            reactionAddedEvents.add(settings)
            scheduledExecutor.schedule({
                if (reactionAddedEvents.contains(settings)) {
                    expiration?.invoke()
                    reactionAddedEvents.remove(settings)
                }
            }, time.toLong(), timeUnit)
            return settings
        }

        fun cancelMessage(settings: WaiterSettings<GuildMessageReceivedEvent>) = messageReceivedEvents.remove(settings)
        fun cancelReactionAdd(settings: WaiterSettings<GuildMessageReactionAddEvent>) = reactionAddedEvents.remove(settings)

        fun check(event: Event) {
            when (event) {
                is GuildMessageReceivedEvent -> {
                    val iterator = messageReceivedEvents.iterator()
                    while (iterator.hasNext()) {
                        val toCheck = iterator.next()
                        if (toCheck.condition(event)) {
                            toCheck.callback(event)
                            iterator.remove()
                        }
                    }
                }
                is GuildMessageReactionAddEvent -> {
                    val iterator = reactionAddedEvents.iterator()
                    while (iterator.hasNext()) {
                        val toCheck = iterator.next()
                        if (toCheck.condition(event)) {
                            toCheck.callback(event)
                            iterator.remove()
                        }
                    }
                }
            }
        }
    }
}

class WaiterSettings<T : Event>(val condition: (T) -> Boolean, val callback: (T) -> Unit, val expiration: (() -> Unit)?)

fun TextChannel.selectFromList(member: Member, title: String, options: List<Any>, consumer: (Int, Message) -> Unit, footer: String? = null,
                               failure: (() -> Unit)? = null, register: ArdentRegister) {
    val language = register.database.getGuildData(member.guild).language ?: Language.ENGLISH
    val embed = getEmbed(title, member.user, member.guild)
    options.forEachIndexed { i, obj -> embed.appendDescription("${Emojis.SMALL_BLUE_DIAMOND} **${i + 1}**: $obj\n") }
    footer?.let { embed.appendDescription("\n$it") }
    embed.appendDescription("\n" + "__${register.translationManager.translate("sender.choose", language)}__" + "\n")

    register.sender.send(embed, null, this, member.user, null, member.guild, callback = { message ->
        for (x in 1..options.size) {
            message.addReaction(when (x) {
                1 -> Emojis.KEYCAP_DIGIT_ONE
                2 -> Emojis.KEYCAP_DIGIT_TWO
                3 -> Emojis.KEYCAP_DIGIT_THREE
                4 -> Emojis.KEYCAP_DIGIT_FOUR
                5 -> Emojis.KEYCAP_DIGIT_FIVE
                6 -> Emojis.KEYCAP_DIGIT_SIX
                7 -> Emojis.KEYCAP_DIGIT_SEVEN
                8 -> Emojis.KEYCAP_DIGIT_EIGHT
                9 -> Emojis.KEYCAP_DIGIT_NINE
                10 -> Emojis.KEYCAP_TEN
                else -> Emojis.HEAVY_CHECK_MARK
            }.symbol).queue()
        }
        message.addReaction(Emojis.HEAVY_MULTIPLICATION_X.symbol).queue()
        var invoked = false
        Sender.waitForMessage({ it.channel.id == id && it.author.id == member.user.id && it.guild.id == guild.id }, { event ->
            if (!invoked) {
                invoked = true
                val responseInt = event.message.contentRaw.toIntOrNull()?.minus(1)
                if (responseInt == null || responseInt !in 0..(options.size - 1)) {
                    register.sender.send(register.translationManager.translate("sender.invalid_response", language),
                            null, this, member.user, null, guild)
                } else {
                    if (options.contains(event.message.contentRaw)) {
                        consumer.invoke(options.indexOf(event.message.contentRaw), message)
                    } else {
                        consumer.invoke(responseInt, message)
                    }
                }
                message.delete().reason("list selection - Ardent").queue()
                event.message.delete().queue()
            }
        })

        Sender.waitForReaction({ it.user.id == member.user.id && it.messageId == message.id }, { event ->
            if (!invoked) {
                invoked = true
                val chosen = when (event.reactionEmote.name) {
                    Emojis.KEYCAP_DIGIT_ONE.symbol -> 1
                    Emojis.KEYCAP_DIGIT_TWO.symbol -> 2
                    Emojis.KEYCAP_DIGIT_THREE.symbol -> 3
                    Emojis.KEYCAP_DIGIT_FOUR.symbol -> 4
                    Emojis.KEYCAP_DIGIT_FIVE.symbol -> 5
                    Emojis.KEYCAP_DIGIT_SIX.symbol -> 6
                    Emojis.KEYCAP_DIGIT_SEVEN.symbol -> 7
                    Emojis.KEYCAP_DIGIT_EIGHT.symbol -> 8
                    Emojis.KEYCAP_DIGIT_NINE.symbol -> 9
                    Emojis.KEYCAP_TEN.symbol -> 10
                    Emojis.HEAVY_MULTIPLICATION_X.symbol -> 69
                    else -> 69999999
                } - 1
                when {
                    chosen in 0..(options.size - 1) -> consumer.invoke(chosen, message)
                    chosen != 68 -> register.sender.send(register.translationManager.translate("sender.invalid_response", language),
                            null, this, member.user, null, guild)
                    else -> {
                        register.sender.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd + register.translationManager.translate("sender.canceled", language),
                                null, this, member.user, null, guild)
                    }
                }
                message.delete().reason("list selection - Ardent").queue()
            }
        }, expiration = {
            if (!invoked) failure?.invoke()
                    ?: register.sender.send(register.translationManager.translate("sender.timeout", language)
                            .apply(20), null, this, member.user, null, member.guild)
            message.delete().queue()
        })
    })
}