package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.format
import com.ardentbot.kotlin.getEmbed
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class ServerInfo : Command("serverinfo", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val guild = event.guild
        val data = register.database.getGuildData(guild)

        val embed = getEmbed("[] | Server Info".apply(guild.name), event.author, event.guild)
                .addField("Number of members", guild.members.size.toString(), true)
                .addField("Online users", guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().format(), true)
                .addField("Prefixes", data.prefixes.joinToString { it.prefix }, true)
                .addField("Admin Role", data.adminRoleId?.let { guild.getRoleById(it) }
                        ?.let { it.name + "(${it.id})" } ?: "None", true)
                .addField("Owner", guild.owner.user.display(), true)
                .addField("Creation Date", guild.creationTime.toLocalDate().toString(), true)
                .addField("# of Voice Channels", guild.voiceChannels.size.toString(), true)
                .addField("# of Text Channels", guild.textChannels.size.toString(), true)
                .addField("# of Roles", guild.roles.size.toString(), true)
                .addField("Region", guild.region.getName(), true)
                .addField("Verification Level", guild.verificationLevel.toString(), true)
        register.sender.cmdSend(embed, this, event)
    }
}