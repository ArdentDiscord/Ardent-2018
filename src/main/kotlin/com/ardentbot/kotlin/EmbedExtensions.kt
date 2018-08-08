package com.ardentbot.kotlin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Sender
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

private val random = Random()

fun getEmbed(title: String, channel: TextChannel, color: Color? = null) = getEmbed(title, channel.guild.selfMember.user, channel.guild, color)

fun getEmbed(title: String, user: User, guild: Guild?, color: Color? = null): EmbedBuilder {
    return EmbedBuilder()
            .setAuthor(title, "https://ardentbot.com", guild?.iconUrl ?: user.jda.selfUser.effectiveAvatarUrl)
            .setColor(color ?: Color(random.nextFloat(), random.nextFloat(), random.nextFloat()))
            .setFooter(user.display(), user.effectiveAvatarUrl)
}

class PaginationEmbed(val user: String?, val dataSetter: (Int) -> EmbedBuilder, val hasPage: (Int) -> Boolean, var page: Int = 1,
                      val expirationTime: Int = 1, val expirationUnit: TimeUnit = TimeUnit.MINUTES, val register: ArdentRegister) {
    fun send(channel: TextChannel) {
        if (!hasPage(page)) throw Exception("Page $page was provided, but hasPage($page) returned false!")
        register.sender.send(dataSetter(page), null, channel, register.selfUser, null, callback = { message ->
            message.addReaction(Emojis.LEFTWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emojis.BLACK_RIGHTWARDS_ARROW.symbol).queue()
            message.addReaction(Emojis.HEAVY_MULTIPLICATION_X.symbol).queue()
            check(message)
        })
    }

    private fun check(message: Message) {
        Sender.waitForReaction({ it.messageId == message.id && user?.equals(it.user.id) ?: true },
                callback = {
                    val left = Emojis.LEFTWARDS_BLACK_ARROW.symbol
                    val right = Emojis.BLACK_RIGHTWARDS_ARROW.symbol
                    when (it.reactionEmote.name) {
                        left, right -> {
                            it.reaction.removeReaction(it.user).queue()

                            if (hasPage(if (it.reactionEmote.name == left) page - 1 else page + 1)) {
                                page = if (it.reactionEmote.name == left) page - 1 else page + 1
                                message.editMessage(dataSetter(page).build()).queue()
                            } else register.sender.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    "That's a non-existant page!", null, message.channel, register.selfUser, null, callback = {
                                it.delete().queueAfter(2, TimeUnit.SECONDS)
                            })
                        }
                        Emojis.HEAVY_MULTIPLICATION_X.symbol -> message.delete().queue()
                    }
                    if (it.reactionEmote.name != Emojis.HEAVY_MULTIPLICATION_X.symbol) check(message)
                }, expiration = { message.delete().queue() }, time = expirationTime, timeUnit = expirationUnit)
    }
}

val MAX_PAGE = { max: Int -> { page: Int -> page in 1..max } }

fun List<String>.embedify(): String {
    if (isEmpty()) throw IllegalArgumentException("The provided list was empty")
    val builder = StringBuilder()
    forEachIndexed { i, s -> if (s.isNotEmpty()) builder.append("\n").append(i.diamond()).append(" ").append(s)}
    return builder.toString()
}