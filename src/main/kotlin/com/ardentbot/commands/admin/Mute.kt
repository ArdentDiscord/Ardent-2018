package com.ardentbot.commands.admin

import com.ardentbot.core.*
import com.ardentbot.core.commands.*
import com.ardentbot.core.database.GuildData
import com.ardentbot.core.database.UserMute
import com.ardentbot.kotlin.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color

@ModuleMapping("admin")
class Mute : Command("mute", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        if (arguments.size < 2) {
            if (arguments.getOrNull(0) == "list") {
                val embed = getEmbed("Mute list", event.author, event.guild)
                register.database.getMutes().filter { event.guild.id == it.guildId }.forEachIndexed { i, mute ->
                    mute.muted.toMember(event.guild)?.user?.let { muted ->
                        embed.appendDescription(i.diamond() + "**" + muted.display() + "**: muted at *"
                                + mute.mutedAt.localeDate() + "* until *" + mute.expiresAt.localeDate() + "* by **" +
                                (mute.adder.toMember(event.guild)?.user?.display() ?: "unknown") + "**")
                        if (mute.reason != null) embed.appendDescription(" (${mute.reason})")
                        embed.appendDescription("\n")
                    }
                }
                register.sender.cmdSend(embed, this, event)
            } else if (arguments.getOrNull(0) == "removerole") {
                val data = register.database.getGuildData(event.guild)
                if (data.muteRoleId == null) register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        "There's no mute role set up", this, event)
                else {
                    data.muteRoleId = null
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                            "Removed the mute role. You'll have to set this up later if you want to mute someone", this, event)
                }
            } else displayHelp(event, arguments, flags, register)
        } else {
            // check if there's a mute role. if not, prompt mute role creation
            val data = register.database.getGuildData(event.guild)
            var role = data.muteRoleId?.let { event.guild.getRoleById(it) }
            if (role == null) {
                register.sender.cmdSend("This server doesn't have a **mute role** set up.", this, event)
                event.channel.selectFromList(event.member, "What would you like to do?",
                        listOf("Create a new role", "Use an existing role"), consumer = { i, _ ->
                    if (i == 1) {
                        register.sender.cmdSend("Please enter the name of an existing role..", this, event)
                        Sender.waitForMessage({
                            it.author.id == event.author.id && event.guild.id == it.guild.id &&
                                    it.channel.id == event.channel.id
                        }, {
                            role = event.guild.getRolesByName(it.message.contentRaw, true).getOrNull(0)
                            if (role != null) {
                                data.muteRoleId = role!!.id
                                register.database.update(data)
                                onInvoke(event, arguments, flags, register)
                            } else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    "Canceling.. you specified an invalid role", this, event)
                        })
                    } else {
                        register.sender.cmdSend("What would you like this role to be called?", this, event)
                        Sender.waitForMessage({
                            it.author.id == event.author.id && event.guild.id == it.guild.id &&
                                    it.channel.id == event.channel.id
                        }, {
                            event.guild.controller.createRole().setName(it.message.contentRaw)
                                    .setPermissions(Permission.MESSAGE_READ).setColor(Color.RED)
                                    .setMentionable(true).queue({
                                        register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                "Successfully created role **[]** ([])".apply(it.name, it.id), this, event)
                                        onInvoke(event, arguments, flags, register)
                                    }, {
                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "Sorry, I couldn't create that role", this, event)
                                    })
                        })
                    }
                }, register = register)
                return
            }

            val user = arguments[0].toUserId().toMember(event.guild)
            if (user == null) {
                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to mention a user to mute",
                        this, event)
                return
            } else if (user.hasPermission(Permission.MANAGE_SERVER)) {
                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You cannot mute this user", this, event)
                return
            }

            val timeUnparsed = arguments[1]
            if (timeUnparsed.isBlank() || !timeUnparsed.contains(Regex("^.+[smhd]\$"))) {
                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        "You must specify a total time for this mute. **Example:** 15h (mute for 15 hours)",
                        this, event)
                return
            }
            val time = arguments[1].substring(0, arguments[1].length - 1).toIntOrNull()
            if (time == null || time <= 0) {
                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        "You specified an invalid length of time", this, event)
                return
            }

            val muteTime = System.currentTimeMillis() + time * 1000 * when (arguments[1].last()) {
                's' -> 1
                'm' -> 60
                'h' -> 60 * 60
                'd' -> 60 * 60 * 24
                else -> throw Exception("how in the world did you do this??")
            }

            event.guild.controller.addSingleRoleToMember(user, role).queue {
                register.database.insert(UserMute(user.user.id, event.guild.id, System.currentTimeMillis(), muteTime,
                        flags.get("r")?.value, event.author.id))
                register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                        "Successfully added the mute role to **[]**".apply(user.user.display()),
                        this, event)
                user.user.openPrivateChannel().queue {
                    it.sendMessage("You've been muted in **[]** until *[]*"
                            .apply(event.guild.name, muteTime.localeDate())).queue()
                }
            }

            event.guild.textChannels.forEach { textChannel ->
                if (role!!.hasPermission(textChannel, Permission.MESSAGE_WRITE)) {
                    try {
                        textChannel.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE)
                                .reason("Mute role setup").queue()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    fun getMuteRole(data: GuildData, event: GuildMessageReceivedEvent): Role? {
        return data.muteRoleId?.let { event.guild.getRoleById(it) }
    }

    val list = ArgumentInformation("list", "list all current mutes")
    val removeRole = ArgumentInformation("removerole", "remove the set mute role")
    val user = ArgumentInformation("@User time", "mention the user you want to mute and the time to mute for: " +
            "Time modifiers: d (days), h (hours), m (minutes)")

    val reason = FlagInformation("r", "reason", "optionally specify a reason for the mute")

    val example = "@User 15h -r for bad sportsmanship"

    val elevated = ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_ROLES))
}