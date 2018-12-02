package com.ardentbot.core.commands

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.database.getUserData
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.diamond
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.kotlin.removeIndices
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import kotlin.collections.set

@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleMapping(val name: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class Excluded

@Retention(AnnotationRetention.RUNTIME)
annotation class MockCommand(val description: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class MockTranslations(vararg val translations: MockTr)


@Retention(AnnotationRetention.RUNTIME)
annotation class MockArguments(vararg val arguments: MockArgument)

/**
 * @param readable instructions for use - leave blank if you don't want to use it
 */
annotation class MockArgument(val id: String, val description: String, val readable: String = "")
/**
 * [id] is whatever is invoked after the command name. access it like CMDNAME.[id]
 */
annotation class MockTr(val id: String, val value: String)

open class Module(val name: String, val id: String)

data class EventParams(val event: GuildMessageReceivedEvent, val command: Command, val arguments: List<String>, val flags: List<Flag>, val register: ArdentRegister) {
    fun translate(id: String): String {
        return register.translationManager.translate(id, register.database.getGuildData(event.guild).language
                ?: Language.ENGLISH)
    }
}

/**
 * Parent class of [Command]s and (soon) Tags. Represents an invokable object, whether object, tag, or other
 */
abstract class Invokable(val name: String, val aliases: Array<String>?, val cooldown: Int?) {
    abstract fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister)
}

abstract class Command(name: String, aliases: Array<String>?, cooldown: Int?) : Invokable(name, aliases, cooldown) {
    lateinit var description: String
    val users = hashMapOf<String, Long>()

    val preconditions = mutableListOf<Precondition>()
    val arguments = mutableListOf<Argument>()
    val flags = mutableListOf<FlagModel>()
    val ex = mutableListOf<String>()

    fun check(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val params = EventParams(event, this, arguments, flags, register)
        preconditions.forEach { precondition ->
            if (!precondition.condition(params)) {
                val replyList = precondition.onFailure(params).map { it.toString() }
                register.sender.send(replyList[0].apply(replyList.toMutableList().removeIndices(0)),
                        this, event.channel, event.author, event)
                return
            }
        }

        val data = register.database.getUserData(event.author)
        data.money += 5
        register.database.update(data)

        val before = System.currentTimeMillis()
        onInvoke(event, arguments, flags, register)
        println("Execution time: ${System.currentTimeMillis() - before}")
    }

    init {
        if (cooldown != null) {
            preconditions.add(Precondition({ params ->
                if (params.event.member.hasPermission(Permission.MANAGE_SERVER)
                        || params.event.member.roles.map { it.id }
                                .contains(params.register.database.getGuildData(params.event.guild).adminRoleId)) true
                else {
                    val lastUsed = params.command.users[params.event.author.id]
                    when {
                        lastUsed == null -> {
                            params.command.users[params.event.author.id] = System.currentTimeMillis()
                            true
                        }
                        lastUsed + 1000 * cooldown < System.currentTimeMillis() -> {
                            params.command.users[params.event.author.id] = System.currentTimeMillis()
                            true
                        }
                        else -> false
                    }
                }
            }, { params ->
                listOf(translate("cooldown.fail", params.event, params.register), params.command.name,
                        params.event.author.asMention,
                        Math.ceil((1000 * cooldown + params.command.users[params.event.author.id]!!
                                - System.currentTimeMillis()) / 1000.0))
            }))
        }
    }

    open fun displayHelp(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val name = this.getTranslatedName(event.guild, register)
        val description = this.getTranslatedDescription(event.guild, register)
        val embed = getEmbed(translate("cmd.help", event, register).apply(name), event.author, event.guild)
                .appendDescription("*${description ?: translate("cmd.nodesc", event, register)+" :("}*")
        if (cooldown != null) embed.appendDescription("\n" + translate("cmd.cooldown", event, register).apply(cooldown) + "\n")
        embed.appendDescription("\n")
        if (this.arguments.isEmpty() && this.flags.isEmpty()) {
            register.sender.cmdSend("**$name**: ${description
                    ?: translate("cmd.nodesc", event, register)}", this, event)
            return
        }
        if (this.arguments.isNotEmpty()) {
            embed.appendDescription("**" + translate("cmd.subcommands", event, register) + "**").appendDescription("\n")
            this.arguments.forEachIndexed { i, argument ->
                val argumentInfo = getTranslatedArgument(argument.id, event.guild, register)
                embed.appendDescription("${i.diamond()} *${argumentInfo.name}*: ${argumentInfo.description}\n")
            }
        }

        if (this.flags.isNotEmpty()) {
            if (this.arguments.isNotEmpty()) embed.appendDescription("\n")
            embed.appendDescription("**" + translate("cmd.flags", event, register) + "**").appendDescription("\n")
            this.flags.forEachIndexed { i, flag ->
                val flagInfo = getTranslatedFlagDescription(flag.id, event.guild, register)
                embed.appendDescription("${i.diamond()}  -**${flag.value}**${if (flagInfo.argumentAccepted == null) "_:_"
                else " _(" + flagInfo.argumentAccepted + "):_"} __${flagInfo.description}__\n")
            }
        }

        if (ex.isNotEmpty()) {
            embed.appendDescription("\n")
                    .appendDescription("__" + (if (ex.size > 1) translate("cmd.examples", event, register)
                    else translate("cmd.example", event, register)) + "__:")
            if (ex.size > 1) embed.appendDescription("\n")
            ex.forEach { embed.appendDescription("/$name *$it*\n") }
            embed.descriptionBuilder.removeSuffix("\n")
        }

        if (aliases?.isNotEmpty() == true) embed.appendDescription("\n" + translate("cmd.aliases", event, register)
                .apply(aliases.joinToString()) + "\n")

        embed.appendDescription("\n" + translate("cmd.allhelp", event, register))

        register.sender.cmdSend(embed, this, event)
    }

    fun invokePrecondition(precondition: Precondition, event: GuildMessageReceivedEvent, arguments: List<String>,
                           flags: List<Flag>, register: ArdentRegister,quiet:Boolean = false): Boolean {
        val params = EventParams(event, this, arguments, flags, register)
        return if (precondition.condition(params)) true
        else {
            if (!quiet)event.channel.send(precondition.onFailure(params).joinToString(""), register)
            false
        }
    }

    fun String.isTranslatedPhrase(id: String, guild: Guild, register: ArdentRegister): Boolean {
        return translate(id, guild, register).equals(this, true) || translate(id, Language.ENGLISH, register).equals(this, true)
    }

    fun translate(id: String, event: GuildMessageReceivedEvent, register: ArdentRegister) = translate(id, event.guild, register)
    fun translate(id: String, guild: Guild, register: ArdentRegister) = translate(id, register.database.getGuildData(guild).language
            ?: Language.ENGLISH, register)

    fun translate(id: String, language: Language, register: ArdentRegister): String {
        return register.translationManager.translate(id, language)
    }

    fun translateNull(id: String, guild: Guild, register: ArdentRegister): String? {
        return translateNull(id, register.database.getGuildData(guild).language ?: Language.ENGLISH, register)
    }

    fun translateNull(id: String, language: Language, register: ArdentRegister): String? {
        return register.translationManager.translateNull(id, language)
    }

    fun getTranslatedName(guild: Guild, register: ArdentRegister) = translate(name, guild, register)
    fun getTranslatedEnglishName(register: ArdentRegister) = translate(name, Language.ENGLISH, register)
    fun getTranslatedDescription(guild: Guild, register: ArdentRegister) = translateNull("$name.description", guild, register)
    fun getTranslatedFlagDescription(flagName: String, guild: Guild, register: ArdentRegister): FlagInformation {
        return FlagInformation(flagName,
                translateNull("$name.flags.$flagName.accepted", guild, register),
                translate("$name.flags.$flagName.description", guild, register))
    }

    fun getTranslatedArgument(argumentName: String, guild: Guild, register: ArdentRegister): ArgumentInformation {
        return getTranslatedArgument(argumentName, register.database.getGuildData(guild).language
                ?: Language.ENGLISH, register) ?: getTranslatedArgument(argumentName, Language.ENGLISH, register)!!
    }

    fun getTranslatedArgument(argumentName: String, language: Language, register: ArdentRegister): ArgumentInformation? {
        val value = translateNull("$name.arguments.$argumentName", language, register)
        //println("argumentName: $argumentName | value: $value | readable: ${translateNull("$name.arguments.$argumentName.readable", language, register)}")
        return ((translateNull("$name.arguments.$argumentName.readable", language, register) ?: value)
                ?.let { ArgumentInformation(it, value, translate("$name.arguments.$argumentName.description", language, register)) })
    }

    fun String.isTranslatedArgument(argumentName: String, guild: Guild, register: ArdentRegister): Boolean {
        val translated = getTranslatedArgument(argumentName, guild, register)
        val english = getTranslatedArgument(argumentName, Language.ENGLISH, register)!!
        return (translated.value ?: translated.name).equals(this, true) || (english.value
                ?: english.name).equals(this, true)
    }
}

// checked at runtime by the PreconditionMapping annotations
class Precondition(val condition: (EventParams) -> Boolean, val onFailure: (EventParams) -> List<Any>)

data class FlagInformation(val name: String, val argumentAccepted: String?, val description: String)
data class ArgumentInformation(val name: String, val value: String?, val description: String)

data class Argument(val id: String)
data class FlagModel(val value: String, val id: String)