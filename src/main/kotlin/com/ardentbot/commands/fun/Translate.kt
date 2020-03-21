package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import com.github.vbauer.yta.model.Language
import com.github.vbauer.yta.service.YTranslateApiImpl
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.lang.reflect.Modifier

@ModuleMapping("fun")
class Translate : Command("translate", null, 5) {
    private val translateApi = YTranslateApiImpl("trnsl.1.1.20170227T013942Z.6878bfdf518abdf6.a6574733436345112da24eb08e7ee1ef2a0d6a97")

    private val languages = Language::class.java.declaredFields.filter { Modifier.isStatic(it.modifiers) }.map { it.get(null) }
            .filter { it is Language }.map { it as Language }

    private fun getLanguage(str: String): Language? = languages.firstOrNull {
        it.name().get().equals(str, true) || it.code().equals(str, true)
    }

    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.getOrNull(0)?.equals("list") == true) {
            val embed = getEmbed(translate("translate.embed_title", event, register), event.author, event.guild)
                    .appendDescription(translate("translate.language_list", event, register).apply("https://ardentbot.com/commands/translate")
                            + "\n\n" + translate("translate.common", event, register))
            register.sender.cmdSend(embed, this, event)
            return
        }
        val toLanguage = arguments.getOrNull(0)?.let { getLanguage(it) }
        if (toLanguage == null || arguments.size < 2) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                translate("translate.invalid_language", event, register) + "\n" +
                translate("translate.e", event, register).apply(example), this, event)
        else {
            val content = arguments.without(0).concat()
            register.sender.cmdSend(translate("translate.response", event, register)
                    .apply(translateApi.translationApi().translate(content, toLanguage)),
                    this, event)

        }
    }

    val example = "french hello world from Ardent!"
}