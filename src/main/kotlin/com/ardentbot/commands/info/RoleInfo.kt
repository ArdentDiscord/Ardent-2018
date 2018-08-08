package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class RoleInfo : Command("roleinfo", arrayOf("ri"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val roles = event.message.mentionedRoles.toMutableList()
        if (roles.isEmpty()) {
            val role = if (arguments.isNotEmpty()) event.guild.getRolesByName(arguments.concat(), true)
                    .getOrNull(0) else null
            if (role == null) {
                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        "You need to mention or enter the name of a role!", this, event)
            } else roles.add(role)
        }
        roles.forEach { role ->
            val embed = getEmbed("[] | Role Info".apply(role.name), event.author, event.guild)
                    .setThumbnail(event.guild.iconUrl)
                    .addField("# with role", "[] members".apply(event.guild.members.filter { it.roles.contains(role) }
                            .count().toString()), true)
                    .addField("Role ID", role.id, true)
                    .addField("Creation Date", (role.creationTime.toEpochSecond() / 1000).localeDate(), true)
                    .addField("Permissions", role.permissions.map { it.getName() }.joinToString(), true)
            register.sender.cmdSend(embed, this, event)
        }
    }
}