package com.ardentbot.core.database

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.connect
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel

class Staff(id: String, val role: StaffRole) : DbObject(id, table = "staff") {
    enum class StaffRole {
        HELPER, MODERATOR, ADMINISTRATOR
    }
}

fun Member.hasPermission(channel: TextChannel, register: ArdentRegister, musicCommand: Boolean = false, failQuietly: Boolean = false): Boolean {
    if (isOwner || hasPermission(channel, Permission.ADMINISTRATOR) || hasPermission(Permission.BAN_MEMBERS)) return true
    else {
        val data = register.database.getGuildData(guild)
        val musicSettings = register.database.getGuildMusicSettings(guild)
        if (!musicCommand) return false else {
            if (voiceState != null && guild.selfMember.voiceState != null && musicSettings.canEveryoneUseAdminCommands || voiceState!!.inVoiceChannel() && guild.selfMember.voiceState!!.inVoiceChannel()) {
                voiceState!!.channel!!.members.size == 2
            } else {
                if (roles.map { it.id }.contains(data.adminRoleId)) return true
                val manager = guild.getAudioManager(channel, register)
                return manager.manager.current?.user == user.id
            }
        }
    }
    if (!failQuietly) channel.send("You need `Administrator` priviledges in this server to be able to use this command", register)
    return false
}


fun Member.checkSameChannel(textChannel: TextChannel?, register: ArdentRegister, complain: Boolean = true): Boolean {
    if (voiceState?.channel == null) {
        textChannel?.send("${Emojis.CROSS_MARK} " + "You need to be connected to a voice channel", register)
        return false
    }
    if (guild.selfMember.voiceState?.channel != voiceState?.channel) {
        return voiceState?.channel?.connect(textChannel, register, complain) == true
    }
    return true
}
