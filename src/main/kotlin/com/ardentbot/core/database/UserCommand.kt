package com.ardentbot.core.database

data class UserCommand(val userId: String, val channelId: String, val guildId: String, val command: String, val time: Long = System.currentTimeMillis())
    : DbObject(table = "commands")