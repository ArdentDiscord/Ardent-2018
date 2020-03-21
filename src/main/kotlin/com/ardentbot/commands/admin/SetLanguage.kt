package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class SetLanguage : Command("lang", arrayOf("language", "setlang"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        val currLang = data.language ?: Language.ENGLISH
        if (arguments.isEmpty()) {
            event.channel.send(translate("lang.current_info",event, register).apply(currLang.readable) + "\n" +
                    "**${translate("lang.language_list",event, register)}**:" + "\n" + Language.values().joinToString("\n") { "${it.readable} (${it.id})" }, register)
        } else {
            val language = Language.values().find { it.id.equals(arguments.concat(), true) || it.readable.equals(arguments.concat(), true) }
            if (language == null) event.channel.send(translate("lang.invalid_lang",event, register), register)
            else {
                event.channel.send(Emojis.WARNING_SIGN.cmd +
                        translate("lang.warning_change",event, register).apply(currLang.readable, language.readable), register)
                Sender.waitForMessage({ it.channel.id == event.channel.id && it.author.id == event.author.id && it.guild.id == event.guild.id }, {
                    if (it.message.contentRaw.startsWith("y", true) || it.message.contentRaw.isTranslatedPhrase("yes", event.guild, register)) {
                        data.language = language
                        register.database.update(data)
                        event.channel.send(Emojis.HEAVY_CHECK_MARK.cmd + translate("lang.updated",event, register), register)
                    } else event.channel.send(Emojis.OK_HAND.cmd + translate("lang.cancel_update",event, register), register)
                })
            }
        }
    }
}