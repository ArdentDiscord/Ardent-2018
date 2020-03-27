package com.ardentbot.kotlin

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role

fun String.toRole(guild: Guild): Role? {
    return try {
        guild.getRoleById(this)
    } catch (e: Exception) {
        null
    }
}
