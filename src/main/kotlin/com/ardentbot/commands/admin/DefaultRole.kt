package com.ardentbot.commands.admin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.selectFromList
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("admin")
class DefaultRole : Command("defaultrole", arrayOf("dr"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (!event.guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            register.sender.cmdSend(Emojis.WARNING_SIGN.cmd + translate("defaultrole.warning", event, register), this, event)
        }

        val data = register.database.getGuildData(event.guild)
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("view", event.guild, register) == true -> {
                val role = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                val embed = getEmbed(translate("defaultrole.defaultrole_capitalized", event, register) + " | Ardent",
                        event.author, event.guild)
                        .appendDescription("**${translate("defaultrole.defaultrole_capitalized", event, register)}:**"
                                + " " + (role?.name ?: translate("general.none", event, register)))
                        .appendDescription("\n\n")

                if (role != null) embed.appendDescription("Total users with this role: **[]**"
                        .apply(event.guild.members.filter { it.roles.contains(role) }.size))
                register.sender.cmdSend(embed, this, event)
            }
            arg?.isTranslatedArgument("set", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                translate("general.specifyrole", event, register), this, event)
                        return
                    }
                    val role = event.message.mentionedRoles.getOrNull(0)
                            ?: event.guild.getRolesByName(arguments.without(0).concat(), true).getOrNull(0)
                    if (role == null) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                translate("general.cant_find_role", event, register), this, event)
                        return
                    }

                    val old = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                    data.defaultRoleId = role.id
                    register.database.update(data)

                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            translate("general.update", event, register)
                                    .apply(translate("defaultrole.defaultrole", event, register), role.name, old?.name
                                            ?: translate("general.none", event, register)),
                            this, event)

                    if (event.guild.members.any { it.roles.isEmpty() }) {
                        register.sender.cmdSend(translate("defaultrole.prompt", event, register), this, event)
                        Sender.scheduledExecutor.schedule({
                            event.channel.selectFromList(event.member!!, translate("defaultrole.title", event, register),
                                    listOf(translate("yes", event, register), translate("no", event, register)),
                                    consumer = { i, _ ->
                                        if (i == 0) {
                                            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                                    translate("defaultrole.creating", event, register)
                                                            .apply(role.name), this, event)
                                            var assigned = 0
                                            event.guild.members.filter { it.roles.isEmpty() }.forEach {
                                                event.guild.addRoleToMember(it, role)
                                                        .reason("Auto-assigning default role").complete()
                                                assigned++
                                            }
                                            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                                    translate("defaultrole.assigned", event, register).apply(assigned), this, event)
                                        } else register.sender.cmdSend(Emojis.OK_HAND.cmd +
                                                translate("defaultrole.created", event, register), this, event)
                                    }, footer = translate("defaultrole.footer", event, register), register = register)
                        }, 2, TimeUnit.SECONDS)
                    }
                }
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val old = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                    data.defaultRoleId = null
                    register.database.update(data)
                    val none = translate("general.none", event, register)
                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            translate("general.update", event, register).apply(translate("defaultrole.defaultrole", event, register),
                                    none, old?.name ?: none), this, event)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val view = Argument("view")
    val set = Argument("set")
    val remove = Argument("remove")

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_ROLES))
}