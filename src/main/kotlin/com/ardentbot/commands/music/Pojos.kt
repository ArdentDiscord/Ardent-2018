package com.ardentbot.commands.music

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.database.DbObject
import com.ardentbot.core.database.asPojo
import com.ardentbot.core.database.getLanguage
import com.ardentbot.core.translation.Language
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel

class DatabaseMusicLibrary(id: String, var tracks: MutableList<DatabaseTrackObj>, var lastModified: Long = System.currentTimeMillis())
    : DbObject(id, "music_libraries") {
    fun load(member: Member, channel: TextChannel, register: ArdentRegister) {
        val vc = member.voiceState?.channel
        if (vc == null) channel.send(register.translationManager.translate("music.not_in_voice",member.guild.getLanguage(register) ?: Language.ENGLISH ), register)
        else if (vc.connect(channel, register)) {
            tracks.forEach { track ->
                track.url.load(member, channel, register, speak = false) { loaded, _ ->
                    DEFAULT_TRACK_LOAD_HANDLER(member,channel,loaded,true,null,null,null,null,register)
                }
            }
            channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + register.translationManager.translate("music.loaded_ardent_tracks",
                    member.guild.getLanguage(register) ?: Language.ENGLISH).apply(tracks.size), register)
        }
    }

}

class DatabaseMusicPlaylist(id: String, val owner: String, var name: String, var lastModified: Long, var spotifyAlbumId: String?,
                            val spotifyPlaylistId: String?, val youtubePlaylistUrl: String?, val tracks: MutableList<DatabaseTrackObj> = mutableListOf())
    : DbObject(id, table = "music_playlists") {
    fun toLocalPlaylist(member: Member): LocalPlaylist {
        return LocalPlaylist(member, this)
    }
}

data class DatabaseTrackObj(val owner: String, val addedAt: Long, val playlistId: String?, val title: String, val author: String, val url: String) {
    fun toDisplayTrack(musicPlaylist: DatabaseMusicPlaylist? = null, lib: DatabaseMusicLibrary? = null): DisplayTrack {
        if (musicPlaylist == null && lib == null) throw IllegalArgumentException("Lib or playlist must be present to convert to display track")
        return DisplayTrack(musicPlaylist?.owner ?: lib!!.id as String, musicPlaylist?.spotifyAlbumId, null,
                null, title, author)
    }
}

data class LocalTrackObj(val user: String, val owner: String, val playlist: LocalPlaylist?, val spotifyPlaylistId: String?, val spotifyAlbumId: String?, val spotifyTrackId: String?, var track: AudioTrack?, var url: String? = track?.info?.uri) {
    fun getUri(): String? {
        return when {
            spotifyPlaylistId != null -> "https://open.spotify.com/user/${spotifyPlaylistId.split(" ")[0]}/playlist/${spotifyPlaylistId.split(" ")[1]}"
            spotifyAlbumId != null -> "https://open.spotify.com/album/$spotifyAlbumId"
            spotifyTrackId != null -> "https://open.spotify.com/track/$spotifyTrackId"
            url != null -> url
            track != null -> track!!.info.uri
            else -> null
        }
    }
}


data class LocalPlaylist(val member: Member, val playlist: DatabaseMusicPlaylist) {
    fun isSpotify(): Boolean = playlist.spotifyAlbumId != null || playlist.spotifyPlaylistId != null
    fun loadTracks(channel: TextChannel, member: Member, register: ArdentRegister) {
        if (playlist.spotifyAlbumId != null) playlist.spotifyAlbumId!!.toSpotifyAlbumUrl().loadSpotifyAlbum(this.member, channel, register, playlist)
        if (playlist.spotifyPlaylistId != null) {
            playlist.spotifyPlaylistId.toSpotifyPlaylistUrl().loadSpotifyPlaylist(this.member, channel, register, playlist)
        }
        if (playlist.youtubePlaylistUrl != null) {
            playlist.youtubePlaylistUrl.loadYoutube(member, channel, register, playlist, lucky = false)
        }
        if (playlist.tracks.size > 0) {
            playlist.tracks.forEach { track ->
                when {
                    track.url.startsWith("https://open.spotify.com/track/") -> track.url.loadSpotifyTrack(member, channel, register, playlist) { audioTrack, id ->
                        play(channel, member, LocalTrackObj(member.user.id, member.user.id, this, null, null, id, audioTrack), register)
                    }
                    else -> track.url.loadYoutube(member, channel, register, playlist, search = false, lucky = false) { found ->
                        DEFAULT_TRACK_LOAD_HANDLER(member, channel, found, true, playlist, null, null, null, register)
                    }
                }

            }
        }
    }
}

data class DisplayTrack(val owner: String, val playlistId: String?, val spotifyAlbumId: String?, val spotifyTrackId: String?,
                        val title: String, val author: String)

data class DisplayPlaylist(val owner: String, val lastModified: Long, val spotifyAlbumId: String?, val spotifyPlaylistId: String?,
                           val youtubePlaylistUrl: String?, val tracks: MutableList<DisplayTrack> = mutableListOf())

data class DisplayLibrary(val owner: String, val lastModified: Long, val tracks: MutableList<DisplayTrack> = mutableListOf())

data class ServerQueue(val voiceId: String, val channelId: String?, val tracks: List<String>)

data class TrackDisplay(val title: String, val author: String)

class LoggedTrack(val guildId: String, val position: Double) : DbObject(table = "music_played")

fun getPlaylistById(id: String, register: ArdentRegister): DatabaseMusicPlaylist? {
    return asPojo(register.database.get("music_playlists", id) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
}

fun String.toSpotifyPlaylistUrl(): String {
    val split = split("||")
    return "https://open.spotify.com/user/${split[0]}/playlist/${split[1]}"
}

fun String.toSpotifyAlbumUrl(): String {
    return "https://open.spotify.com/album/$this"
}