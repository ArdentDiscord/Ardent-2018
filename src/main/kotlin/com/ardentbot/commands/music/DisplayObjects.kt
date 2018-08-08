package com.ardentbot.commands.music

import com.ardentbot.core.ArdentRegister

// Getting information about tracks
fun getUserPlaylistInformation(playlistId: String, register: ArdentRegister): DisplayPlaylist? {
    val playlist = getPlaylistById(playlistId, register) ?: return null
    val display = DisplayPlaylist(playlist.owner, playlist.lastModified, playlist.spotifyAlbumId, playlist.spotifyPlaylistId, playlist.youtubePlaylistUrl)
    playlist.tracks.forEach { display.tracks.add(it.toDisplayTrack(playlist)) }
    return display
}

fun getUserLibrary(id: String, register: ArdentRegister): DisplayLibrary? {
    val lib = register.database.getMusicLibrary(id)
    return if (lib.tracks.isEmpty()) null
    else {
        val display = DisplayLibrary(id, lib.lastModified)
        lib.tracks.forEach { display.tracks.add(it.toDisplayTrack(null, lib)) }
        display
    }
}