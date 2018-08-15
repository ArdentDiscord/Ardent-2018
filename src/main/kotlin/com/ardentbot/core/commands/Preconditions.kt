package com.ardentbot.core.commands

import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.apply
import net.dv8tion.jda.core.Permission

val ELEVATED_PERMISSIONS = { permissions: List<Permission> ->
    Precondition(condition = {
        it.event.member.hasPermission(permissions) ||
                it.register.database.getGuildData(it.event.guild).let { data ->
                    data.adminRoleId != null && it.event.guild.getRoleById(data.adminRoleId) != null
                            && it.event.member.roles.contains(it.event.guild.getRoleById(data.adminRoleId))
                }
    }, onFailure = {
        listOf(it.register.translationManager.translate("precondition.no_permission",
                it.register.database.getGuildData(it.event.guild).language ?: Language.ENGLISH)
                .apply(permissions.joinToString { permission -> permission.getName() }))
    })
}