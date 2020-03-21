package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.format
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class ServerInfo : Command("serverinfo", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val guild = event.guild
        val data = register.database.getGuildData(guild)

        val embed = getEmbed(translate("serverinfo.embed_title",event, register).apply(guild.name), event.author, event.guild)
                .addField(translate("serverinfo.num_members",event, register), guild.members.size.toString(), true)
                .addField(translate("serverinfo.online",event, register), guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().format(), true)
                .addField(translate("general.prefixes",event, register), data.prefixes.joinToString { it.prefix }, true)
                .addField(translate("serverinfo.admin_role",event, register), data.adminRoleId?.let { guild.getRoleById(it) }
                        ?.let { it.name + "(${it.id})" } ?: translate("general.none",event, register), true)
                .addField(translate("serverinfo.owner",event, register), guild.owner?.user?.display(), true)
                .addField(translate("serverinfo.creation",event, register), guild.timeCreated.toLocalDate().toString(), true)
                .addField(translate("serverinfo.num_voice",event, register), guild.voiceChannels.size.toString(), true)
                .addField(translate("serverinfo.num_text",event, register), guild.textChannels.size.toString(), true)
                .addField(translate("serverinfo.num_roles",event, register), guild.roles.size.toString(), true)
                .addField(translate("serverinfo.region",event, register), guild.region.getName(), true)
                .addField(translate("serverinfo.verification_level",event, register), guild.verificationLevel.toString(), true)
        register.sender.cmdSend(embed, this, event)
    }
}