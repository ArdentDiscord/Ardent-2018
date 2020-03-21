package com.ardentbot.core

import com.ardentbot.commands.games.send
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class ExternalAction {
    companion object {
        val currentlyUsedExternalActions = ConcurrentLinkedQueue<Pair<String /* URL */, Long>>()
        val waitingCallbacks = ConcurrentLinkedQueue<Pair<String /* URL */, (Any) -> Unit>>()

        fun waitSpotify(member: Member, channel: TextChannel, register: ArdentRegister, callback: (Any) -> Unit) {
            wait(member.user.id, member, channel, register, callback)
        }

        fun waitDateTime(member: Member, channel: TextChannel, register: ArdentRegister, callback: (Any) -> Unit): String {
            val path = "/dynamic/datetime/${register.random.nextLong()}/${register.random.nextLong()}"
            wait(path, member, channel, register, callback)
            return path
        }

        fun wait(path: String, member: Member, channel: TextChannel, register: ArdentRegister, callback: (Any) -> Unit) {
            val obj = Pair(path, callback)
            waitingCallbacks.add(obj)

            Sender.scheduledExecutor.schedule({
                if (waitingCallbacks.contains(obj)) {
                    waitingCallbacks.remove(obj)
                    currentlyUsedExternalActions.removeIf { it.first == path }
                    channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                            register.translationManager.translate("external.timeout",
                                    register.database.getGuildData(member.guild).language ?: Language.ENGLISH)
                                    .apply(member.asMention), register)
                }
                currentlyUsedExternalActions.removeIf { it.first == path }
            }, 5, TimeUnit.MINUTES)
        }
    }
}