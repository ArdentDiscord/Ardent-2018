package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.FlagInformation
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.GuildData
import com.ardentbot.core.get
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.OnlineStatus.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class Online : Command("online", null, null) {
    fun OnlineStatus.toEmoji(): String {
        return (when (this) {
            ONLINE -> Emojis.GREEN_APPLE
            OFFLINE -> Emojis.MEDIUM_WHITE_CIRCLE
            DO_NOT_DISTURB -> Emojis.LARGE_RED_CIRCLE
            IDLE -> Emojis.LARGE_ORANGE_DIAMOND
            else -> Emojis.BLACK_QUESTION_MARK_ORNAMENT
        }).cmd
    }

    private fun getAdminsOf(guild: Guild, data: GuildData): List<Member> {
        return guild.members.filter { it.hasPermission(Permission.ADMINISTRATOR) || it.roles.map { it.id }.contains(data.adminRoleId) }
    }

    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "types" -> {
                val embed = getEmbed("Discord Status Types | Ardent", event.author, event.guild)
                        .appendDescription("**Name** | **Flag argument**")
                        .appendDescription(
                                listOf("Online: *online*",
                                        "Offline: *offline*",
                                        "Do Not Disturb: *dnd*, *donotdisturb*, *nodisturb*",
                                        "Idle: *idle*",
                                        "Unknown: *unknown*").embedify()
                        )
                register.sender.cmdSend(embed, this, event)
            }
            "stats" -> {
                val user = event.message.mentionedUsers.getOrNull(0) ?: event.author
                val statusData = getStatusData(user.id, register)
                val total = (statusData.dndTime + statusData.idleTime + statusData.offlineTime + statusData.onlineTime).toFloat()
                val embed = getEmbed("Status Information | []".apply(user.display()), event.author, event.guild)
                        .appendDescription("**Current Status**: *[]*, for *[]*".apply(statusData.current.key, statusData.currentTime.timeDisplay()))
                        .appendDescription("\n")
                if (statusData.statusSize > 1) {
                    embed.appendDescription("**Previous Status**: *[]*, for *[]*".apply(statusData.last.key, statusData.lastTime.timeDisplay()))
                            .appendDescription("\n")
                            .appendDescription(
                                    listOf(if (statusData.onlineTime > 0) "__Online__: " + statusData.onlineTime.timeDisplay() + " (" + (statusData.onlineTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                            if (statusData.dndTime > 0) "__Do Not Disturb__: " + statusData.dndTime.timeDisplay() + " (" + (statusData.dndTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                            if (statusData.idleTime > 0) "__Idle__: " + statusData.idleTime.timeDisplay() + " (" + (statusData.idleTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                            if (statusData.offlineTime > 0) "__Offline__: " + statusData.offlineTime.timeDisplay() + " (" + (statusData.offlineTime * 100 / total).withDecimalPlaceCount(1) + "%)" else ""
                                    ).embedify()
                            )
                            .appendDescription("\n\n")
                            .appendDescription(Emojis.INFORMATION_SOURCE.cmd + "I'm aware of []'s last **[]** status updates".apply(user.asMention, statusData.statusSize))
                            .appendDescription("\n")
                            .appendDescription("Total tracked time: **[]**".apply(total.toLong().timeDisplay()))
                }
                register.sender.cmdSend(embed, this, event)
            }
            "server" -> {
                when (arguments.getOrNull(1)) {
                    "admins" -> {
                        val statuses = getAdminsOf(event.guild, register.database.getGuildData(event.guild)).groupBy { it.onlineStatus }
                                .sort().map { (status, members) ->
                                    "**" + status.key + "** (${status.toEmoji()}): " +
                                            (if (members.size > 20 || status == INVISIBLE)
                                                members.size.toString() + " members"
                                            else "[" + members.joinToString { it.asMention } + "]" + "(${members.size})") + "\n"
                                }

                        val embed = getEmbed("Admins' Online Status | []".apply(event.guild.name), event.author, event.guild)
                                .appendDescription(statuses.embedify())
                                .appendDescription("\n")
                                .appendDescription("See a specific member's online statistics with */online stats @User*")
                        register.sender.cmdSend(embed, this, event)
                    }
                    else -> {
                        val rolesToFilter = flags.get("r")?.values?.mapNotNull { event.guild.getRolesByName(it, true).getOrNull(0) }
                        val statusesToFilter = flags.get("status")?.values?.mapNotNull {
                            when (it) {
                                "online" -> ONLINE
                                "offline" -> OFFLINE
                                "idle" -> IDLE
                                "dnd", "donotdisturb", "nodisturb" -> DO_NOT_DISTURB
                                else -> null
                            }
                        }

                        val grouped = event.guild.members.groupBy { it.onlineStatus }.filter { statusesToFilter == null || statusesToFilter.contains(it.key) }
                                .map {
                                    if (rolesToFilter == null) it.toPair() else it.key to it.value.filter {
                                        var found = false
                                        it.roles.forEach { if (rolesToFilter.contains(it)) found = true }
                                        found
                                    }
                                }.toMap()

                        val admins = getAdminsOf(event.guild, register.database.getGuildData(event.guild)).groupBy { it.onlineStatus }
                        val statuses =
                                grouped.sort().map { (status, members) ->
                                    "**" + status.key + "** (${status.toEmoji()}): " +
                                            (if (members.size > 20 || status == INVISIBLE)
                                                members.size.toString() + " members"
                                            else "[" + members.joinToString { it.asMention } + "]" + "(${members.size})") +
                                            (if (rolesToFilter != null && admins[status]?.isNotEmpty() == true) "\n" + " - ${admins[status]!!.size} " + " admins have this status"
                                            else "") + "\n"
                                }

                        val embed = getEmbed("Online Status | []".apply(event.guild.name), event.author, event.guild)
                                .appendDescription(statuses.embedify())
                                .appendDescription("\n")
                                .appendDescription("See a specific member's online statistics with */online stats @User*")
                        register.sender.cmdSend(embed, this, event)
                    }
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val stats = ArgumentInformation("stats @User",
            "see online/offline statistics about a member. if no user is mentioned, your own stats will be displayed")
    val server = ArgumentInformation("server (admins)", "see status statistics about members in this server. if admins is specified, only their statistics will be shown. " +
            "otherwise, the entire server's will")
    val types = ArgumentInformation("types", "see the possible Discord statuses")

    val type = FlagInformation("status", "status type", "specify a status to filter by - only available for /online server!")
    val role = FlagInformation("r", "role name", "filter by role - only available with /online server!")

    val example = "server -status online -r Moderators"
    val example2 = "server -status \"dnd\" \"idle\""
}

private fun Map<OnlineStatus, List<Member>>.sort(): List<Pair<OnlineStatus, List<Member>>> {
    val list = mutableListOf<Pair<OnlineStatus, List<Member>>>()
    if (containsKey(ONLINE)) list.add(ONLINE to get(ONLINE)!!)
    if (containsKey(DO_NOT_DISTURB)) list.add(DO_NOT_DISTURB to get(DO_NOT_DISTURB)!!)
    if (containsKey(IDLE)) list.add(IDLE to get(IDLE)!!)
    if (containsKey(OFFLINE)) list.add(OFFLINE to get(OFFLINE)!!)
    if (containsKey(INVISIBLE)) list.add(INVISIBLE to get(INVISIBLE)!!)
    return list
}

private fun getStatusData(id: String, register: ArdentRegister): StatusData {
    val statusChanges = register.database.getStatusChanges(id)
    var onlineTime = 0L
    var idleTime = 0L
    var dndTime = 0L
    var offlineTime = 0L
    (0..(statusChanges.size - 1)).forEach { i ->
        val temp = statusChanges[i]
        val timeSpent = if (i == 0) 0 else (statusChanges.getOrNull(i + 1)?.time
                ?: System.currentTimeMillis()) - temp.time
        when (temp.newStatus) {
            ONLINE -> onlineTime += timeSpent
            IDLE -> idleTime += timeSpent
            DO_NOT_DISTURB -> dndTime += timeSpent
            else -> offlineTime += timeSpent
        }
        if (i == statusChanges.lastIndex) {
            return StatusData(id, onlineTime, dndTime, idleTime, offlineTime, temp.newStatus, System.currentTimeMillis() - temp.time, statusChanges[i - 1].newStatus,
                    temp.time - statusChanges[i - 1].time, statusChanges.size)
        }
    }
    throw Exception("how the fuck did this happen? status data retrieval failed")
}

data class StatusData(val id: String, val onlineTime: Long, val dndTime: Long, val idleTime: Long,
                      val offlineTime: Long, val current: OnlineStatus, val currentTime: Long, val last: OnlineStatus,
                      val lastTime: Long, val statusSize: Int)
