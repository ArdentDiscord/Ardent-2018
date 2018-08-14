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
                        translate("general.need_to_mention_role", event, register), this, event)
            } else roles.add(role)
        }
        roles.forEach { role ->
            val embed = getEmbed(translate("roleinfo.embed_title", event, register).apply(role.name), event.author, event.guild)
                    .setThumbnail(event.guild.iconUrl)
                    .addField(translate("roleinfo.num_with_role", event, register), "[] ".apply(event.guild.members.filter { it.roles.contains(role) }
                            .count().toString() + translate("general.members", event, register)), true)
                    .addField(translate("roleinfo.id", event, register), role.id, true)
                    .addField(translate("roleinfo.creation", event, register), (role.creationTime.toEpochSecond() / 1000).localeDate(), true)
                    .addField(translate("roleinfo.permissions", event, register), role.permissions.joinToString { it.getName() }, true)
            register.sender.cmdSend(embed, this, event)
        }
    }
}