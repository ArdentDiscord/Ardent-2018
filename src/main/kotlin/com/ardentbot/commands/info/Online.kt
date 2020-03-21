package com.ardentbot.commands.info

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.FlagModel
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.DbObject
import com.ardentbot.core.database.GuildData
import com.ardentbot.core.get
import com.ardentbot.kotlin.*
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
class Online : Command("online", null, null) {
    fun OnlineStatus.toEmoji(): String {
        return (when (this) {
            OnlineStatus.ONLINE -> Emojis.GREEN_APPLE
            OnlineStatus.OFFLINE -> Emojis.MEDIUM_WHITE_CIRCLE
            OnlineStatus.DO_NOT_DISTURB -> Emojis.LARGE_RED_CIRCLE
            OnlineStatus.IDLE -> Emojis.LARGE_ORANGE_DIAMOND
            else -> Emojis.BLACK_QUESTION_MARK_ORNAMENT
        }).cmd
    }

    private fun getAdminsOf(guild: Guild, data: GuildData): List<Member> {
        return guild.members.filter { it.hasPermission(Permission.ADMINISTRATOR) || it.roles.map { role -> role.id }.contains(data.adminRoleId) }
    }

    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "types" -> {
                val embed = getEmbed(translate("online.embed_title", event, register), event.author, event.guild)
                        .appendDescription(translate("online.types", event, register))
                        .appendDescription(
                                listOf(translate("online.type_online", event, register).apply("online"),
                                        translate("online.type_offline", event, register).apply("offline"),
                                        translate("online.type_dnd", event, register).apply("*dnd*, *donotdisturb*, *nodisturb*"),
                                        translate("online.type_idle", event, register).apply("idle"),
                                        translate("online.type_unknown", event, register).apply("unknown")).embedify()
                        )
                register.sender.cmdSend(embed, this, event)
            }
            "stats" -> {
                val user = event.message.mentionedUsers.getOrNull(0) ?: event.author
                val statusData = register.database.getStatusInfo(user.id)
                        ?: {
                            val data = StatusData(user.id, 0, 0, 0, 0,
                                    event.guild.getMember(user)!!.onlineStatus, System.currentTimeMillis())
                            register.database.insert(data)
                            data
                        }.invoke()
                val currTime = System.currentTimeMillis() - statusData.currentSwitchTime

                when (event.guild.getMember(user)?.onlineStatus) {
                    OnlineStatus.ONLINE -> statusData.onlineTime += currTime
                    OnlineStatus.IDLE -> statusData.idleTime += currTime
                    OnlineStatus.DO_NOT_DISTURB -> statusData.dndTime += currTime
                    OnlineStatus.OFFLINE -> statusData.offlineTime += currTime
                }

                val total = (statusData.dndTime + statusData.idleTime + statusData.offlineTime + statusData.onlineTime).toFloat()
                val embed = getEmbed(translate("online.status_info", event, register).apply(user.display()), event.author, event.guild)
                        .appendDescription(translate("online.current_status", event, register).apply(statusData.current.key, currTime.timeDisplay()))
                        .appendDescription("\n")
                if (listOf(statusData.dndTime, statusData.onlineTime, statusData.offlineTime, statusData.idleTime).filter { it > 0 }.isNotEmpty()) {
                    embed.appendDescription(
                            listOf(if (statusData.onlineTime > 0) "__" + translate("online.online_str", event, register) + "__: " + statusData.onlineTime.timeDisplay() + " (" + (statusData.onlineTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                    if (statusData.dndTime > 0) "__" + translate("online.dnd_str", event, register) + "__: " + statusData.dndTime.timeDisplay() + " (" + (statusData.dndTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                    if (statusData.idleTime > 0) "__" + translate("online.idle_str", event, register) + "__: " + statusData.idleTime.timeDisplay() + " (" + (statusData.idleTime * 100 / total).withDecimalPlaceCount(1) + "%)" else "",
                                    if (statusData.offlineTime > 0) "__" + translate("online.offline_str", event, register) + "__: " + statusData.offlineTime.timeDisplay() + " (" + (statusData.offlineTime * 100 / total).withDecimalPlaceCount(1) + "%)" else ""
                            ).embedify()
                    )
                            .appendDescription("\n\n")
                            .appendDescription(Emojis.INFORMATION_SOURCE.cmd + translate("online.aware_info", event, register).apply(user.asMention, statusData.statusSize + 1))
                            .appendDescription("\n")
                            .appendDescription(translate("online.total_tracked_time", event, register).apply(total.toLong().timeDisplay()))
                }
                register.sender.cmdSend(embed, this, event)
            }
            "server" -> {
                when (arguments.getOrNull(1)) {
                    "admins" -> {
                        val statuses = getAdminsOf(event.guild, register.database.getGuildData(event.guild)).groupBy { it.onlineStatus }
                                .sort().map { (status, members) ->
                                    "**" + status.key + "** (${status.toEmoji()}): " +
                                            (if (members.size > 20 || status == OnlineStatus.INVISIBLE)
                                                members.size.toString() + " " + translate("general.members", event, register)
                                            else "[" + members.joinToString { it.asMention } + "]" + "(${members.size})") + "\n"
                                }

                        val embed = getEmbed(translate("online.admins_status_title", event, register).apply(event.guild.name), event.author, event.guild)
                                .appendDescription(statuses.embedify())
                                .appendDescription("\n")
                                .appendDescription(translate("online.see_member_stats", event, register))
                        register.sender.cmdSend(embed, this, event)
                    }
                    else -> {
                        val rolesToFilter = flags.get("r")?.values?.mapNotNull { event.guild.getRolesByName(it, true).getOrNull(0) }
                        val statusesToFilter = flags.get("status")?.values?.mapNotNull {
                            when (it) {
                                "online" -> OnlineStatus.ONLINE
                                "offline" -> OnlineStatus.OFFLINE
                                "idle" -> OnlineStatus.IDLE
                                "dnd", "donotdisturb", "nodisturb" -> OnlineStatus.DO_NOT_DISTURB
                                else -> null
                            }
                        }

                        val grouped = event.guild.members.groupBy { it.onlineStatus }.filter { statusesToFilter == null || statusesToFilter.contains(it.key) }
                                .map {
                                    if (rolesToFilter == null) it.toPair() else it.key to it.value.filter { member ->
                                        var found = false
                                        member.roles.forEach { role -> if (rolesToFilter.contains(role)) found = true }
                                        found
                                    }
                                }.toMap()

                        val admins = getAdminsOf(event.guild, register.database.getGuildData(event.guild)).groupBy { it.onlineStatus }
                        val statuses =
                                grouped.sort().map { (status, members) ->
                                    "**" + status.key + "** (${status.toEmoji()}): " +
                                            (if (members.size > 20 || status == OnlineStatus.INVISIBLE)
                                                members.size.toString() + " " + translate("general.members", event, register)
                                            else "[" + members.joinToString { it.asMention } + "]" + "(${members.size})") +
                                            (if (rolesToFilter != null && admins[status]?.isNotEmpty() == true) "\n" + " - ${admins[status]!!.size} " + " " + translate("online.admins_count", event, register)
                                            else "") + "\n"
                                }

                        val embed = getEmbed(translate("online.status_embed", event, register).apply(event.guild.name), event.author, event.guild)
                                .appendDescription(statuses.embedify())
                                .appendDescription("\n")
                                .appendDescription(translate("online.see_member_stats", event, register))
                        register.sender.cmdSend(embed, this, event)
                    }
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val stats = Argument("stats")
    val server = Argument("server")
    val types = Argument("types")

    val type = FlagModel("t", "type")
    val role = FlagModel("r", "role")

    val example = "server -status online -r Moderators"
    val example2 = "server -status \"dnd\" \"idle\""
}

private fun Map<OnlineStatus, List<Member>>.sort(): List<Pair<OnlineStatus, List<Member>>> {
    val list = mutableListOf<Pair<OnlineStatus, List<Member>>>()
    if (containsKey(OnlineStatus.ONLINE)) list.add(OnlineStatus.ONLINE to get(OnlineStatus.ONLINE)!!)
    if (containsKey(OnlineStatus.DO_NOT_DISTURB)) list.add(OnlineStatus.DO_NOT_DISTURB to get(OnlineStatus.DO_NOT_DISTURB)!!)
    if (containsKey(OnlineStatus.IDLE)) list.add(OnlineStatus.IDLE to get(OnlineStatus.IDLE)!!)
    if (containsKey(OnlineStatus.OFFLINE)) list.add(OnlineStatus.OFFLINE to get(OnlineStatus.OFFLINE)!!)
    if (containsKey(OnlineStatus.INVISIBLE)) list.add(OnlineStatus.INVISIBLE to get(OnlineStatus.INVISIBLE)!!)
    return list
}

data class StatusData(val id1: String, var onlineTime: Long, var dndTime: Long, var idleTime: Long,
                      var offlineTime: Long, var current: OnlineStatus, var currentSwitchTime: Long,
                      var statusSize: Int = 0) : DbObject(id = id1, table = "status_changes")
