package com.ardentbot.core.database

import net.dv8tion.jda.core.OnlineStatus

data class StatusUpdate(val userId: String, val newStatus: OnlineStatus, val time: Long = System.currentTimeMillis())
    : DbObject(table = "status_changes")