package com.ardentbot.core.translation

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Sender
import com.ardentbot.kotlin.extractFolder
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TranslationManager(val register: ArdentRegister, val languages: MutableList<ArdentLanguage> = mutableListOf()) {
    fun addTranslation(id: String, value: String) {
        languages.first { it.language == Language.ENGLISH }.translations.add(
                ArdentTranslation(id, "", value, value)
        )
    }

    fun translateNull(id: String, language: Language): String? {
        return (languages.first { language == it.language }.translations.find { id == it.identifier }?.translation
                ?: languages.first { it.language == Language.ENGLISH }.translations.find { id == it.identifier }?.translation)
                ?.replace("\\n", "\n")
    }

    fun translate(id: String, language: Language): String = translateNull(id, language) ?: {
        if (language != Language.ENGLISH) translate(id, Language.ENGLISH)
        else {
            register.getTextChannel(register.config["error_channel"])!!.send("Attempted to access nonexistant id $id in language $language", register)
            id
        }
    }.invoke()

    init {
        getTranslations()
        Sender.scheduledExecutor.scheduleAtFixedRate({ getTranslations() }, 5, 5, TimeUnit.MINUTES)
    }

    fun getTranslations() {
        val translationPath = "/translations/all"

        val file = File("/translations/all.zip")
        if (file.exists() && register.config.values["project_crowdin_key"] == null) {
            extractFolder("/translations/all.zip")
        } else {
            file.createNewFile()
            val output = FileOutputStream(file)
            output.write(Jsoup.connect("https://api.crowdin.com/api/project/ardent/download/all.zip?key=${register.config["project_crowdin_key"]}")
                    .ignoreContentType(true).execute().bodyAsBytes())
            output.close()
            extractFolder("/translations/all.zip")
            file.delete()
        }

        val translationRoot = File(translationPath)
        translationRoot.listFiles().forEach { translationDirectory ->
            val language = Language.values().first { it.id == translationDirectory.name }
            val translations = mutableListOf<ArdentTranslation>()

            val translationFile = translationDirectory.listFiles()[0]
            val parser = CSVParser.parse(translationFile, Charset.defaultCharset(), CSVFormat.DEFAULT)

            val englishTranslations = if (languages.find { it.language == Language.ENGLISH } == null) mutableListOf<ArdentTranslation>()
            else null

            parser.forEach { record ->
                try {
                    val id = record[0]
                    val context = record[1]
                    val english = record[2]
                    val translated = record[3]
                    if (english != translated) {
                        translations.add(ArdentTranslation(id, context, english, translated))
                    }
                    englishTranslations?.add(ArdentTranslation(id, context, english, english))
                } catch (ignored: IndexOutOfBoundsException) {
                }
            }
            languages.add(ArdentLanguage(language, translations))
            if (englishTranslations != null) languages.add(ArdentLanguage(Language.ENGLISH, englishTranslations))
        }
        translationRoot.deleteRecursively()
    }
}

data class ArdentLanguage(val language: Language, var translations: MutableList<ArdentTranslation>)
data class ArdentTranslation(val identifier: String, val context: String, val english: String, val translation: String)

enum class Language(val readable: String, val id: String, val maturity: LanguageMaturity) {
    ENGLISH("English", "en", LanguageMaturity.MATURE), FRENCH("Français", "fr", LanguageMaturity.INFANCY),
    GERMAN("Deutsch", "de", LanguageMaturity.INFANCY), CROATIAN("Hrvatski", "hr", LanguageMaturity.INFANCY),
    CYRILLIC_SERBIAN("Cyrillic Serbian", "sr", LanguageMaturity.INFANCY), SPANISH("Español", "es-ES", LanguageMaturity.INFANCY),
    RUSSIAN("ру́сский язы́к", "ru", LanguageMaturity.INFANCY), DUTCH("Nederlands", "nl", LanguageMaturity.INFANCY),
    POLISH("Polskie", "pl", LanguageMaturity.INFANCY), BRAZILIAN_PORTUGUESE("Portugues do Brasil", "pt-BR", LanguageMaturity.INFANCY),
    HINDI("हिंदी", "hi", LanguageMaturity.INFANCY);

    override fun toString() = readable
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"), IN_DEVELOPMENT("In Development"), NEEDS_REFINING("Needs Refining"),
    MATURE("Mature");

    override fun toString() = readable
}