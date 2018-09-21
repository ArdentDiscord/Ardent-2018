package com.ardentbot.core

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.events.user.update.UserUpdateOnlineStatusEvent

class StatusUpdateChanger {
    companion object {
        fun change(event: UserUpdateOnlineStatusEvent, register: ArdentRegister) {
            register.cachedExecutor.execute {
                try {
                    val info = register.database.getStatusInfo(event.user.id) ?: return@execute
                    when (event.oldOnlineStatus) {
                        OnlineStatus.ONLINE -> info.onlineTime += System.currentTimeMillis() - info.currentSwitchTime
                        OnlineStatus.OFFLINE -> info.offlineTime += System.currentTimeMillis() - info.currentSwitchTime
                        OnlineStatus.DO_NOT_DISTURB -> info.dndTime += System.currentTimeMillis() - info.currentSwitchTime
                        OnlineStatus.IDLE -> info.idleTime += System.currentTimeMillis() - info.currentSwitchTime
                        else -> return@execute
                    }
                    info.current = event.newOnlineStatus
                    info.currentSwitchTime = System.currentTimeMillis()
                    info.statusSize++
                    register.database.update(info)
                }catch (ignored:Exception){
                }
            }
        }
    }
}