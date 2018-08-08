package com.ardentbot.core.utils

import com.ardentbot.core.ArdentRegister

class ErrorService(val ardent: ArdentRegister) {
    /*
    fun invoke(channel: MessageChannel, command: Command?, e: Throwable) {
        channel.send("Oh no, an error occured! We've logged this instance and alerted the developers. " +
                "If you'd like to provide more detail, you can join our Discord at https://ardentbot.com/discord")
        e.printStackTrace()
        ardent.jda.getTextChannelById(419283618976759809).send(
                "**Time**: ${DateFormat.getDateTimeInstance().format(Date.from(Instant.now()))}\n" +
                        "**Channel ID**: ${channel.id} ${if (channel is TextChannel) "**(${channel.guild.name})**" else ""}\n" +
                        "**Exception**:\n   ${ExceptionUtils.getMessage(e).shortenTo(750)}" +
                        if (command != null) "\n**Command**: ${command.primaryName}" else ""
        )
        ardent.database.r.table("exceptions").insert(ardent.database.gson.toJson(
                LoggedException(ExceptionUtils.getMessage(e), System.currentTimeMillis(), channel.idLong, command?.primaryName
                        ?: "none")
        )).runNoReply(ardent.database.conn)
    }*/
}