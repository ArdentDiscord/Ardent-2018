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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.lang.management.ManagementFactory

@ModuleMapping("ardent")
class Status : Command("status", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val internals = Internals(register)
        val embed = getEmbed("Status | Ardent", event.author, event.guild)
                .addField("Loaded Commands", internals.commandCount.toString(), true)
                .addField("Messages Received", internals.messagesReceived.format(), true)
                .addField("Commands Received", internals.commandsReceived.format(), true)
                .addField("Servers", internals.guilds.format(), true)
                .addField("Users", internals.users.format(), true)
                .addField("Loaded Music Players", internals.loadedMusicPlayers.format(), true)
                .addField("CPU Usage", "${internals.cpuUsage}%", true)
                .addField("RAM Usage", "${internals.usedRam} / ${internals.totalRam} mb", true)
                .addField("Uptime", internals.uptimeFancy, true)
                .addField("Servers w/Ardent as only bot", internals.onlyBot.format() +
                        " (${(internals.onlyBot * 100 / register.getAllGuilds().size.toFloat()).format()}%)", true)
                .addField("Website", "https://ardentbot.com", true)
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
    val loadedMusicPlayers: Int = managers.filter { it.value.manager.current != null || it.value.player.playingTrack != null }.count()
}