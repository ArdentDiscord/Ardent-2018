package com.ardentbot.core.database

import com.ardentbot.core.ArdentRegister

class UserData(id: String, var name: String?, var aboutMe: String?, var from: String?,
               var languages: MutableList<String>? = null, var money: Long = 0,
               val dailyCollectedAt: MutableList<Long> = mutableListOf(-1), var spotifyId: String? = null) : DbObject(id, table = "users") {
    fun getSpotifyUser(register: ArdentRegister) = spotifyId?.let { register.spotifyApi.users.getProfile(it).complete() }
}

data class Marriage(val first: String, val second: String, val established: Long) : DbObject(table = "marriages")