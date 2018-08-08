package com.ardentbot.core

import com.ardentbot.core.database.StatusUpdate
import net.dv8tion.jda.core.events.user.update.UserUpdateOnlineStatusEvent

class StatusUpdateChanger {
    companion object {
        fun change(event: UserUpdateOnlineStatusEvent, register: ArdentRegister) {
            register.database.insert(StatusUpdate(event.user.id, event.newOnlineStatus), blocking = false)
        }
    }
}