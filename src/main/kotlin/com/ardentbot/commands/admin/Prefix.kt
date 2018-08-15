package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.ArdentPrefix
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
class Prefix : Command("prefix", arrayOf("p"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.isEmpty()) {
            displayHelp(event, arguments, flags, register)
            return
        }

        val data = register.database.getGuildData(event.guild)

        val isAdd = arguments.getOrNull(0)?.isTranslatedArgument("add", event.guild, register) == true
        val isRemove = arguments.getOrNull(0)?.isTranslatedArgument("remove", event.guild, register) == true
        val isList = arguments.getOrNull(0)?.isTranslatedArgument("list", event.guild, register) == true

        when {
            isAdd || isRemove -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size > 1) {
                        val prefix = arguments.without(0).joinToString(" ")
                        val exists = data.prefixes.firstOrNull { it.prefix == prefix } != null
                        when {
                            isAdd -> {
                                if (exists) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                        translate("prefix.exists", event, register), this, event)
                                else {
                                    data.prefixes.add(ArdentPrefix(prefix, event.author.id, System.currentTimeMillis()))
                                    register.database.update(data)
                                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                            translate("prefix.added", event, register).apply(prefix, event.guild.name), this, event)
                                }
                            }
                            isRemove -> {
                                if (!exists) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                        translate("prefix.not_exists", event, register), this, event)
                                else {
                                    data.prefixes.removeIf { it.prefix == prefix }
                                    register.database.update(data)
                                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                            translate("prefix.removed", event, register).apply(prefix, event.guild.name), this, event)
                                }
                            }
                        }
                    } else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                            translate("prefix.need_argument", event, register).apply(arguments[0]), this, event)
                }
            }
            isList -> {
                val embed = getEmbed(translate("prefix.embed_title", event, register).apply(event.guild.name), event.author, event.guild)
                        .appendDescription("**${translate("general.prefixes", event, register)}**:")
                data.prefixesModified(register).forEach { prefix ->
                    embed.appendDescription("\n")
                            .appendDescription(translate("prefix.row", event, register).apply(prefix.prefix,
                                    prefix.adder.toMember(event.guild)?.user?.display()
                                            ?: translate("unknown", event, register),
                                    prefix.addDate.localeDate()))
                }
                register.sender.cmdSend(embed, this, event)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val add = Argument("add")
    val remove = Argument("remove")
    val list = Argument("list")

    val example = "add !"
}