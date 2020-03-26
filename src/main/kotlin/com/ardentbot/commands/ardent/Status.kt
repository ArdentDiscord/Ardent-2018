package com.ardentbot.commands.ardent

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.managers
import com.ardentbot.kotlin.format
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.kotlin.getProcessCpuLoad
import com.ardentbot.kotlin.timeDisplay
import com.ardentbot.web.base
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.lang.management.ManagementFactory

@ModuleMapping("ardent")
class Status : Command("status", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val internals = Internals(register)
        val embed = getEmbed(translate("status.embed_title", event, register), event.author, event.guild)
                .addField(translate("status.loaded_commands", event, register), internals.commandCount.toString(), true)
                .addField(translate("status.messages_received", event, register), internals.messagesReceived.format(), true)
                .addField(translate("status.commands_received", event, register), internals.commandsReceived.format(), true)
                .addField(translate("servers", event, register), internals.guilds.format(), true)
                .addField(translate("users", event, register), internals.users.format(), true)
                .addField(translate("status.loaded_music_players", event, register), internals.loadedMusicPlayers.format(), true)
                .addField(translate("status.cpu", event, register), "${internals.cpuUsage}%", true)
                .addField(translate("status.ram", event, register), "${internals.usedRam} / ${internals.totalRam} mb", true)
                .addField(translate("status.uptime", event, register), internals.uptimeFancy, true)
                .addField(translate("status.servers_only_bot", event, register), internals.onlyBot.format() +
                        " (${(internals.onlyBot * 100 / register.getAllGuilds().size.toFloat()).format()}%)", true)
                .addField(translate("status.website", event, register), base, true)
        register.sender.cmdSend(embed, this, event)
    }
}

class Internals(register: ArdentRegister) {
    val commandCount = register.holder.commands.size
    val messagesReceived = register.processor.receivedMessages
    val commandsReceived = register.processor.receivedCommands
    val guilds = register.getAllGuilds().size
    val users = register.getAllUsers().size
    val cpuUsage = getProcessCpuLoad()
    val totalRam = Runtime.getRuntime().totalMemory() / 1024 / 1024
    val usedRam = totalRam - Runtime.getRuntime().freeMemory() / 1024 / 1024
    val apiCalls = register.getApiCalls()
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val uptimeFancy: String
        get() = uptime.timeDisplay()
    val onlyBot = register.getAllGuilds().filter { guild -> guild.members.count { it.user.isBot } == 1 }.count()
    val loadedMusicPlayers: Int = managers.filter { it.value.player.playingTrack != null }.count()
}