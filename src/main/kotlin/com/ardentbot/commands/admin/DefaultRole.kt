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
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("admin")
class DefaultRole : Command("defaultrole", arrayOf("dr"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (!event.guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            register.sender.cmdSend(Emojis.WARNING_SIGN.cmd +
                    "Warning! I don't have the `Manage Roles` permission, so I cannot administer the default role!", this, event)
        }

        val data = register.database.getGuildData(event.guild)
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("view", event.guild, register) == true -> {
                val role = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                val embed = getEmbed("Default Role | Ardent", event.author, event.guild)
                        .appendDescription("**Default role:**" + " " + (role?.name ?: "None"))
                        .appendDescription("\n\n")

                if (role != null) embed.appendDescription("Total users with this role: **[]**"
                        .apply(event.guild.members.filter { it.roles.contains(role) }.size))
                register.sender.cmdSend(embed, this, event)
            }
            arg?.isTranslatedArgument("set", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    if (arguments.size == 1) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                "You need to specify a role name or mention a role!", this, event)
                        return
                    }
                    val role = event.message.mentionedRoles.getOrNull(0)
                            ?: event.guild.getRolesByName(arguments.without(0).concat(), true).getOrNull(0)
                    if (role == null) {
                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                "I couldn't find a role with that name. Please try again", this, event)
                        return
                    }

                    val old = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                    data.defaultRoleId = role.id
                    register.database.update(data)

                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            "You set the default role to: **[]**\nPrevious default role: **[]**"
                                    .apply(role.name, old?.name ?: "None"), this, event)

                    if (event.guild.members.any { it.roles.isEmpty() }) {
                        register.sender.cmdSend("**Ardent Helpers** > Would you like to assign members with no roles to this role?", this, event)
                        Sender.scheduledExecutor.schedule({
                            event.channel.selectFromList(event.member, "Ardent Helpers | Auto Assign", listOf("Yes", "No"),
                                    consumer = { i, _ ->
                                        if (i == 0) {
                                            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                                    "Created default role **[]**. Assigning users now.. (this may take a while)"
                                                            .apply(role.name), this, event)
                                            var assigned = 0
                                            event.guild.members.filter { it.roles.isEmpty() }.forEach {
                                                event.guild.controller.addSingleRoleToMember(it, role)
                                                        .reason("Auto-assigning default role").complete()
                                                assigned++
                                            }
                                            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                                    "Assigned **[]** members to the default role".apply(assigned), this, event)
                                        } else register.sender.cmdSend("Ok, default role created" + " " +
                                                Emojis.OK_HAND.symbol, this, event)
                                    }, footer = "If you enjoy Ardent, join our hub server! Type /hub or /support!", register = register)
                        }, 2, TimeUnit.SECONDS)
                    }
                }
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val old = data.defaultRoleId?.let { event.guild.getRoleById(it) }
                    data.defaultRoleId = null
                    register.database.update(data)

                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            "You set the default role to: **[]**\nPrevious default role: **[]**"
                                    .apply("None", old?.name ?: "None"), this, event)
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