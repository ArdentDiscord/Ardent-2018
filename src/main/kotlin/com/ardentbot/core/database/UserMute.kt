package com.ardentbot.core.database

data class UserMute(val muted: String, val guildId: String, val mutedAt: Long, val expiresAt: Long, val reason: String?,
                    val adder: String) : DbObject(table = "mutes")