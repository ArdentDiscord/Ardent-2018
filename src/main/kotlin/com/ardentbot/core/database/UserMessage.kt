package com.ardentbot.core.database

class UserMessage(val userId: String, val channelId: String, val guildId: String, val content: String, val time: Long)
    : DbObject(table = "logs")