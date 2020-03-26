package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.*
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ELEVATED_PERMISSIONS
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.DbObject
import com.ardentbot.core.database.getLanguage
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.*
import com.ardentbot.web.base
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

val appendedAnnouncements = mutableListOf<AppendedAnnouncement>()

data class AppendedAnnouncement(val text: String, val sentTo: MutableList<String> = mutableListOf())

@ModuleMapping("admin")
class Announce : Command("announce", arrayOf("announcement", "announcements"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val arg = arguments.getOrNull(0)
        when {
            (arg == "global" || arg == "appended") && event.author.id == "169904324980244480" -> {
                val text = arguments.without(0)
                if (text.isEmpty()) event.channel.send("That message is empty!", register)
                else event.channel.send(Emojis.WARNING_SIGN.cmd + "Are you sure you want to send the following $arg message:\n${text.joinToString(" ")}",
                        register) { _ ->
                    Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id }, { confirmationEvent ->
                        if (confirmationEvent.message.contentRaw == "yes") {
                            if (arg == "global") {
                                register.getAllGuilds().forEach { guild ->
                                    Sender.waitForMessage({ it.guild.id == guild.id }, {
                                        try {
                                            it.channel.sendMessage(Emojis.INFORMATION_SOURCE.cmd +
                                                    register.translationManager.translate("sender.announce", it.guild.getLanguage(register)
                                                            ?: Language.ENGLISH) +
                                                    " " + text.joinToString(" ")).queue()
                                        } catch (ignored: Exception) {
                                        }
                                    }, time = 24, timeUnit = TimeUnit.HOURS)
                                }
                                event.channel.send("started sending", register)
                            } else {
                                appendedAnnouncements.add(AppendedAnnouncement(text.joinToString(" ")))
                                event.channel.send("added", register)
                            }
                        }
                    })
                }
            }
            arg?.isTranslatedArgument("create", event.guild, register) == true -> {
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    event.channel.send("Starting announcement creation.. set options can be changed later with /announce edit", register)
                    after(2, {
                        event.channel.send("What channel would you like to send this announcement to? Please mention it", register)
                        Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { channelEvent ->
                            if (channelEvent.message.mentionedChannels.isEmpty()) {
                                register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                        "You needed to mention a channel. Canceling setup..", this, event)
                            } else {
                                val channel = channelEvent.message.mentionedChannels[0]
                                event.channel.send("What would you like this announcement to say? You can use the following special tags: []"
                                        .apply("\n" +
                                                listOf("**[start]**: the start time of this announcement",
                                                        "**[end]**: the end time of this announcement",
                                                        "**[now]**: the current time",
                                                        "*new* **[until-end]**: the time remaining until the end of this announcement",
                                                        "__Warning__: [until-end] is only available for announcements that repeat multiple times"
                                                ).embedify() + "\n\n" +
                                                "You have **5** minutes to enter the announcement message. Type `cancel` to cancel."), register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { messageEvent ->
                                    if (messageEvent.message.contentRaw.equals("cancel", true)) {
                                        event.channel.send(Emojis.OK_HAND.cmd + "Canceling..", register)
                                    } else {
                                        val message = messageEvent.message.contentRaw
                                        event.channel.send("How many times would you like this announcement to repeat? Type `none` if you don't want it to repeat at all or `always` to always repeat", register)
                                        Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { repeatAmtMessage ->
                                            var repeat = when {
                                                repeatAmtMessage.message.contentRaw.equals("none", true) -> 0
                                                repeatAmtMessage.message.contentRaw.equals("always", true) -> 99999
                                                else -> repeatAmtMessage.message.contentRaw.toIntOrNull()
                                            }
                                            if (repeatAmtMessage.message.contentRaw.toIntOrNull() == null && repeat == null) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                    "You didn't provide a valid announcement repetition amount. Canceling..", register)
                                            else {
                                                if (repeat != null && repeat < 0) repeat = 0
                                                else if (repeat == 99999) repeat = null
                                                if (repeat == 0) promptStartTime(event, register, channel, message, repeat, null)
                                                else {
                                                    event.channel.send(
                                                            "How long should there be between announcements? Acceptable formats: **minute** (m), **hour** (h), and **day** (d)." +
                                                                    "\n" + "**Examples**: *10h* (10 hours) | *4m* (4 minutes)", register)
                                                    Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { timeBetweenMessage ->
                                                        val content = timeBetweenMessage.message.contentRaw
                                                        val multiplier = when (content.lastOrNull()) {
                                                            'm' -> 1
                                                            'h' -> 60
                                                            'd' -> 60 * 24
                                                            else -> null
                                                        }
                                                        val time = if (content.length > 1) content.substring(0, content.length - 1).toIntOrNull() else null
                                                        if (multiplier == null || time == null) {
                                                            event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                                    "You didn't provide a valid time period. Canceling..", register)
                                                        } else promptStartTime(event, register, channel, message, repeat, time * 1000 * 60 * multiplier.toLong())
                                                    }, {
                                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                                "You didn't respond in time. Canceled announcement setup", this, event)
                                                    }, 30)
                                                }
                                            }
                                        }, {
                                            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                    "You didn't respond in time. Canceled announcement setup", this, event)
                                        }, 30)
                                    }
                                }, {
                                    register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                            "You didn't respond in time. Canceled announcement setup", this, event)
                                }, 5, TimeUnit.MINUTES)
                            }
                        }, {
                            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    "You didn't respond in time. Canceled announcement setup", this, event)
                        }, 30)
                    })
                }
            }
            arg?.isTranslatedArgument("edit", event.guild, register) == true -> {
                val announcements = register.database.getAnnouncements(event.guild)
                val selected = arguments.getOrNull(1)?.toIntOrNull()?.let { announcements.getOrNull(it - 1) }
                if (selected == null) {
                    event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a valid announcement number", register)
                    return
                }
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    event.channel.send("Current announcement data: []".apply("\n" + selected.display(event, register)), register)
                    after(2, {
                        event.channel.selectFromList(event.member!!, "Which field would you like to change for this announcement?",
                                listOf("Message", "Channel", "Start time", "Repetitions", "Time between repetitions"), { i, _ ->
                            when (i) {
                                0 -> {
                                    event.channel.send("What would you like this announcement to say? You can use the following special tags: []"
                                            .apply("\n" +
                                                    listOf("**[start]**: the start time of this announcement",
                                                            "**[end]**: the end time of this announcement",
                                                            "**[now]**: the current time",
                                                            "*new* **[until-end]**: the time remaining until the end of this announcement",
                                                            "__Warning__: [until-end] is only available for announcements that repeat multiple times"
                                                    ).embedify() + "\n\n" +
                                                    "You have **5** minutes to enter the announcement message. Type `cancel` to cancel."), register)
                                    Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { messageEvent ->
                                        if (messageEvent.message.contentRaw.equals("cancel", true)) {
                                            event.channel.send(Emojis.OK_HAND.cmd + "Canceling..", register)
                                        } else {
                                            val message = messageEvent.message.contentRaw
                                            val old = selected.message
                                            selected.message = message

                                            selected.editedByLast = event.author.id
                                            selected.editedByTime = System.currentTimeMillis()

                                            register.database.update(selected)
                                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                    "You set the announcement message to: **[]**\nPrevious message: **[]**"
                                                            .apply(message, old), this, event)
                                        }
                                    }, {
                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "You didn't respond in time. Canceling..", this, event)
                                    }, 30)
                                }
                                1 -> {
                                    event.channel.send("Mention the channel you'd like to send the announcement to", register)
                                    Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { channelEvent ->
                                        if (channelEvent.message.mentionedChannels.isEmpty()) {
                                            register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                    "You needed to mention a channel. Canceling..", this, event)
                                        } else {
                                            val channel = channelEvent.message.mentionedChannels[0]

                                            val old = selected.channel
                                            selected.channel = channel.id

                                            selected.editedByLast = event.author.id
                                            selected.editedByTime = System.currentTimeMillis()

                                            register.database.update(selected)
                                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                    "You set the announcement channel to: **[]**\nPrevious channel: **[]**"
                                                            .apply(channel.asMention, old.toChannel(event.guild)?.asMention
                                                                    ?: "unknown"), this, event)
                                        }
                                    }, {
                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "You didn't respond in time. Canceling..", this, event)
                                    }, 30)
                                }
                                2 -> {
                                    val path = ExternalAction.waitDateTime(event.member!!, event.channel, register) {
                                        val time = it as Long
                                        if (System.currentTimeMillis() > time) {
                                            event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You specified a time that's already happened! Canceling..", register)
                                        } else {
                                            val old = selected.startTime
                                            selected.startTime = time

                                            selected.editedByLast = event.author.id
                                            selected.editedByTime = System.currentTimeMillis()

                                            register.database.update(selected)
                                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                    "You set the announcement start time to: **[]**\nPrevious start time: **[]**"
                                                            .apply(time.localeDate(), old.localeDate()), this, event)
                                        }
                                    }
                                    event.channel.send("The link is: []".apply("$base$path"), register)
                                }
                                3 -> {
                                    event.channel.send("How many repetitions should there be?", register)
                                    Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { repetitionAmountMessage ->
                                        val repetitions = repetitionAmountMessage.message.contentRaw?.toIntOrNull()
                                        if (repetitions == null || repetitions < 0) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "You didn't provide a valid announcement repetition amount. Canceling..", register)
                                        else {
                                            val old = selected.repetitionAmountLeft
                                            selected.repetitionAmountLeft = repetitions

                                            selected.editedByLast = event.author.id
                                            selected.editedByTime = System.currentTimeMillis()

                                            register.database.update(selected)
                                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                    "You set the announcement repetitions to: **[]**\nPrevious repetition amount: **[]**"
                                                            .apply(repetitions, old ?: "No end"), this, event)
                                        }
                                    }, {
                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "You didn't respond in time. Canceling..", this, event)
                                    }, 30)
                                }
                                4 -> {
                                    event.channel.send(
                                            "How long should there be between announcements? Acceptable formats: **minute** (m), **hour** (h), and **day** (d)." +
                                                    "\n" + "**Examples**: *10h* (10 hours) | *4m* (4 minutes)", register)
                                    Sender.waitForMessage({ it.author.id == event.author.id && it.guild.id == event.guild.id }, { timeBetweenMessage ->
                                        val content = timeBetweenMessage.message.contentRaw
                                        val multiplier = when (content.lastOrNull()) {
                                            'm' -> 1
                                            'h' -> 60
                                            'd' -> 60 * 24
                                            else -> null
                                        }
                                        val time = if (content.length > 1) content.substring(0, content.length - 1).toIntOrNull() else null
                                        if (multiplier == null || time == null) {
                                            event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                    "You didn't provide a valid time period. Canceling..", register)
                                        } else {
                                            val old = selected.timeBetweenRepetitions
                                            selected.timeBetweenRepetitions = time * 1000 * 60 * multiplier.toLong()

                                            selected.editedByLast = event.author.id
                                            selected.editedByTime = System.currentTimeMillis()

                                            register.database.update(selected)
                                            register.sender.cmdSend(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                                    "You set the time between repetitions to: **[]**\nPrevious: **[]**"
                                                            .apply(selected.timeBetweenRepetitions!!.timeDisplay(),
                                                                    old?.timeDisplay() ?: "None"), this, event)
                                        }
                                    }, {
                                        register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                                "You didn't respond in time. Canceling..", this, event)
                                    }, 30)
                                }
                            }
                        }, register = register)
                    })
                }
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                val announcements = register.database.getAnnouncements(event.guild)
                val selected = arguments.getOrNull(1)?.toIntOrNull()?.let { announcements.getOrNull(it - 1) }
                if (selected == null) {
                    event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a valid announcement number", register)
                    return
                }
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    register.database.delete(selected)
                    event.channel.send(Emojis.HEAVY_CHECK_MARK.cmd + "Removed the announcement.", register)
                }
            }
            arg?.isTranslatedArgument("list", event.guild, register) == true -> {
                val announcements = register.database.getAnnouncements(event.guild)
                if (announcements.isEmpty()) event.channel.send("There aren't any announcements!", register)
                else {
                    val embed = getEmbed("Announcements | []".apply(event.guild.name), event.author, event.guild)
                    embed.appendDescription(announcements.mapIndexed { i, announcement ->
                        "[**[]**] | Message: *[]*".apply(i + 1, announcement.message)
                    }.embedify())
                            .appendDescription("\n\n")
                            .appendDescription("View detailed information about each announcement with /announce info *number*")
                    event.channel.send(embed, register)
                }
            }
            arg?.isTranslatedArgument("info", event.guild, register) == true -> {
                val announcements = register.database.getAnnouncements(event.guild)
                val selected = arguments.getOrNull(1)?.toIntOrNull()?.let { announcements.getOrNull(it - 1) }
                if (selected == null) {
                    event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a valid announcement number", register)
                    return
                }
                val embed = getEmbed("Announcement []".apply(announcements.indexOf(selected) + 1), event.author, event.guild)
                        .appendDescription(selected.display(event, register))
                event.channel.send(embed, register)
            }
            arg?.isTranslatedArgument("trigger", event.guild, register) == true -> {
                val announcements = register.database.getAnnouncements(event.guild)
                val selected = arguments.getOrNull(1)?.toIntOrNull()?.let { announcements.getOrNull(it - 1) }
                if (selected == null) {
                    event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You need to specify a valid announcement number", register)
                    return
                }
                if (invokePrecondition(ELEVATED_PERMISSIONS(listOf(Permission.MANAGE_SERVER)), event, arguments, flags, register)) {
                    val channel = event.guild?.getTextChannelById(selected.channel)
                    if (channel == null) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                            "I was unable to announce ```[]``` in **[]** because the channel provided is invalid."
                                    .apply(selected.message, event.guild.name) + " " + "Canceled announcement..", register)
                    else {
                        event.channel.send(Emojis.HEAVY_CHECK_MARK.cmd +
                                "Sent announcement. Note that this does not update the *last announced at* field", register)
                        channel.send(selected.getEditedMessage(), register)
                    }
                }
            }
            arg?.isTranslatedArgument("params", event.guild, register) == true -> {
                val embed = getEmbed("Announcement Parameters", event.author, event.guild)
                embed.appendDescription(listOf("**[start]**: the start time of this announcement",
                        "**[end]**: the end time of this announcement",
                        "**[now]**: the current time",
                        "*new* **[until-end]**: the time remaining until the end of this announcement",
                        "__Warning__: [until-end] is only available for announcements that repeat multiple times"
                ).embedify())
                event.channel.send(embed, register)
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    private fun promptStartTime(event: GuildMessageReceivedEvent, register: ArdentRegister, channel: TextChannel, message: String,
                                repeatAmount: Int?
                                , timeBetweenRepetitions: Long
            ?
    ) {
        event.channel.send("**Congrats!** You're almost done. Only one more thing: let's specify when you want the announcement to begin!" + "\n" +
                "Go to the provided link, and enter the date and time you'd like! The link will be valid for the next **5** minutes.", register)
        val path = ExternalAction.waitDateTime(event.member!!, event.channel, register) {
            val time = it as Long
            if (System.currentTimeMillis() > time) {
                event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd + "You specified a time that's already happened! Restarting..", register)
                after(1, { promptStartTime(event, register, channel, message, repeatAmount, timeBetweenRepetitions) })
            } else {
                val announcement = Announcement(message, channel.id, time, null, timeBetweenRepetitions,
                        repeatAmount, event.author.id, System.currentTimeMillis(), event.guild.id)
                register.database.insert(announcement)
                event.channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + "Successfully added your announcement. Here's a quick summary: []"
                        .apply("\n" + announcement.display(event, register)), register)
            }
        }
        event.channel.send("The link is: []".apply("$base$path"), register)
    }

    val example = "create"
    val example2 = "remove 4 (remove the 4th announcement as listed in /announce list)"

    val params = Argument("params")
    val create = Argument("create")
    val edit = Argument("edit")
    val remove = Argument("remove")
    val list = Argument("list")
    val view = Argument("info")
    val trigger = Argument("trigger")
}

data class Announcement(var message: String, var channel: String, var startTime: Long, var lastAnnouncementTime: Long?,
                        var timeBetweenRepetitions: Long?, var repetitionAmountLeft: Int? /* null is infinite */,
                        val authorId: String, val creationTime: Long, val guildId: String, var editedByLast: String? = null,
                        var editedByTime: Long? = null) : DbObject(table = "announcements") {
    fun getEditedMessage(): String {
        val end = when {
            repetitionAmountLeft == null -> null
            timeBetweenRepetitions == null -> System.currentTimeMillis()
            else -> ((lastAnnouncementTime ?: startTime)
                    + (repetitionAmountLeft!! * timeBetweenRepetitions!!))
        }
        return message.replace("[start]", startTime.localeDate())
                .replace("[end]", end?.localeDate() ?: "Never")
                .replace("[until-end]", end?.let {
                    var diff = it - System.currentTimeMillis()
                    if (diff < 0) diff = 0
                    diff.timeDisplay()
                } ?: if (repetitionAmountLeft == null) "Never" else "Invalid [until-end] expression!")
                .replace("[now]", System.currentTimeMillis().localeDate())
    }

    fun display(event: GuildMessageReceivedEvent, register: ArdentRegister): String {
        val end = when {
            repetitionAmountLeft == null -> null
            timeBetweenRepetitions == null -> System.currentTimeMillis()
            else -> ((lastAnnouncementTime ?: startTime)
                    + (repetitionAmountLeft!! * timeBetweenRepetitions!!))
        }
        return listOf("**Message**:" + " " + message,
                "**Channel**:" + " " + (channel.toChannel(event.guild)?.asMention ?: "invalid channel"),
                "**Start time**:" + " " + startTime.localeDate(),
                "**End time**:" + " " + (end?.localeDate() ?: "Never"),
                if (lastAnnouncementTime != null) "**Last announced at**:" + " " + lastAnnouncementTime!!.localeDate() else "",
                if (timeBetweenRepetitions != null) "**Repetition Cooldown**:" + " " + timeBetweenRepetitions!!.timeDisplay() else "",
                "**Repetitions Left**:" + " " + if (repetitionAmountLeft == null) "Always" else repetitionAmountLeft,
                "**Created by**:" + " " + (authorId.toUser(register)?.display() ?: "unknown"),
                "**Created at**:" + " " + creationTime.localeDate(),
                if (editedByLast != null) "**Last edited by**:" + " " +
                        (editedByLast!!.toUser(register)?.display() ?: "unknown") else "",
                if (editedByTime != null) "**Last edited at**:" + " " + editedByTime!!.localeDate() else ""
        ).filterNot { it.isBlank() }.embedify()
    }
}