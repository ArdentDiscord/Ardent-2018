package com.ardentbot.commands.music


import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.database.getLanguage
import com.ardentbot.core.playerManager
import com.ardentbot.core.selectFromList
import com.ardentbot.core.translation.Language
import com.ardentbot.core.youtube
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import org.apache.commons.lang3.exception.ExceptionUtils

// Load music when not run from a command
fun String.loadExternally(register: ArdentRegister, consumerFoundTrack: (AudioTrack, String?) -> Unit) {
    val guild = register.getGuild("351220166018727936")!!
    load(guild.selfMember, guild.textChannels[0], register, consumerFoundTrack = consumerFoundTrack, speak = false)
}

// Loading music directly (for use in playing)
val DEFAULT_TRACK_LOAD_HANDLER: (Member, TextChannel, AudioTrack, Boolean, DatabaseMusicPlaylist?, String?
        /* Spotify Playlist Id */, String? /* Spotify Album Id */, String? /* Spotify track id */, ArdentRegister) -> Unit =
        { member, channel, track, isQuiet, musicPlaylist, spotifyPlaylist, spotifyAlbum, spotifyTrack, register ->
            if (!isQuiet) channel.send("${Emojis.BALLOT_BOX_WITH_CHECK} " + (if (member.user.id == member.guild.selfMember.user.id) "**[Autoplay]**" else "")
                    + register.translationManager.translate("music.add_to_queue", member.guild.getLanguage(register)
                    ?: Language.ENGLISH).apply(track.info.title, track.info.author, track.getDurationString()), register)
            play(channel, member, LocalTrackObj(member.user.id, musicPlaylist?.owner
                    ?: member.user.id, musicPlaylist?.toLocalPlaylist(member),
                    spotifyPlaylist ?: musicPlaylist?.spotifyPlaylistId, spotifyAlbum
                    ?: musicPlaylist?.spotifyAlbumId, spotifyTrack, track), register)
        }

val DEFAULT_YOUTUBE_PLAYLIST_LOAD_HANDLER: (Member, TextChannel, AudioPlaylist, Boolean, DatabaseMusicPlaylist?, ArdentRegister) -> Unit =
        { member, channel, tracksPlaylist, isQuiet, musicPlaylist, register ->
            if (!isQuiet) channel.send(register.translationManager.translate("music.add_youtube_playlist", member.guild.getLanguage(register)
                    ?: Language.ENGLISH).apply(tracksPlaylist.name, tracksPlaylist.tracks.size), register)
            tracksPlaylist.tracks.forEach { track ->
                play(channel, member, LocalTrackObj(member.user.id, musicPlaylist?.owner
                        ?: member.user.id, musicPlaylist?.toLocalPlaylist(member),
                        musicPlaylist?.spotifyPlaylistId, musicPlaylist?.spotifyAlbumId, null, track), register)
            }
        }

fun String.load(member: Member, channel: TextChannel, register: ArdentRegister, lucky: Boolean = false, speak: Boolean = true, consumerFoundTrack: ((AudioTrack, String?) -> Unit)? = null) {
    when {
        this.startsWith("https://open.spotify.com/album/") -> this.split("?")[0].loadSpotifyAlbum(member, channel, register = register, consumerFoundTrack = consumerFoundTrack)
        this.startsWith("https://open.spotify.com/track/") -> this.split("?")[0].loadSpotifyTrack(member, channel, consumerFoundTrack = consumerFoundTrack, register = register, speak = speak)
        this.startsWith("https://open.spotify.com/") -> this.split("?")[0].loadSpotifyPlaylist(member, channel, register = register, consumerFoundTrack = consumerFoundTrack)
        else -> {
            if (consumerFoundTrack == null) loadYoutube(member, channel, register, lucky = lucky, speak = speak)
            else loadYoutube(member, channel, consumer = { consumerFoundTrack.invoke(it, null) }, register = register, lucky = lucky, speak = speak)
        }
    }
}

fun String.loadYoutube(member: Member, channel: TextChannel, register: ArdentRegister, musicPlaylist: DatabaseMusicPlaylist? = null, search: Boolean = false, lucky: Boolean, speak: Boolean=true,consumer: ((AudioTrack) -> Unit)? = null) {
    val autoplay = member == member.guild.selfMember
    val language = member.guild.getLanguage(register) ?: Language.ENGLISH
    playerManager.loadItemOrdered(member.guild.getAudioManager(channel, register), this, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            if (consumer != null) consumer.invoke(track)
            else DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, false, musicPlaylist, null, null, null, register)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            try {
                if (playlist.isSearchResult) {
                    when {
                        consumer != null -> {
                            consumer.invoke(playlist.tracks[0])
                            return
                        }
                        autoplay -> {
                            val track = playlist.tracks[0]
                            channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                    register.translationManager.translate("music.autoplay_response", language) + " " +
                                    register.translationManager.translate("music.add_to_queue", language)
                                            .apply(track.info.title, track.info.author, track.getDurationString()), register)
                            DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, true, musicPlaylist, null, null, null, register)
                        }
                        else -> {
                            if (lucky) {
                                DEFAULT_TRACK_LOAD_HANDLER(member, channel, playlist.tracks[0], false, musicPlaylist, null, null, null, register)
                            } else {
                                val selectFrom = mutableListOf<String>()
                                val num: Int = if (playlist.tracks.size >= 7) 7 else playlist.tracks.size
                                (1..num)
                                        .map { playlist.tracks[it - 1] }
                                        .map { it.info }
                                        .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
                                channel.selectFromList(member, register.translationManager.translate("music.select_song", language), selectFrom, { response, _ ->
                                    val track = playlist.tracks[response]
                                    DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, false, musicPlaylist, null, null, null, register)
                                }, register = register)
                            }
                        }
                    }
                } else {
                    DEFAULT_YOUTUBE_PLAYLIST_LOAD_HANDLER(member, channel, playlist, false, musicPlaylist, register)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun loadFailed(exception: FriendlyException) {
            if (exception.localizedMessage.contains("Something went wrong") || exception.localizedMessage.contains("503")) {
                val results = this@loadYoutube.removePrefix("ytsearch:").searchYoutubeOfficialApi(register)
                if (results == null || results.isEmpty()) channel.send(register.translationManager.translate("music.not_found_youtube", language), register)
                else {
                    if (!autoplay) channel.selectFromList(member, register.translationManager.translate("music.select_song", language), results.map { it.first }.toMutableList(),
                            { response, _ ->
                                "https://www.youtube.com/watch?v=${results[response].second}"
                                        .loadYoutube(member, channel, register, musicPlaylist, false, lucky = lucky, speak = speak)
                            }, register = register)
                    else "https://www.youtube.com/watch?v=${results[0].second}".loadYoutube(member, channel, register, musicPlaylist, false, lucky = lucky, speak = speak)
                }
            } else channel.send(register.translationManager.translate("error.something_went_wrong", language).apply(exception.localizedMessage), register)
        }

        override fun noMatches() {
            if (search) {
                if (!autoplay) channel.send(register.translationManager.translate("music.track_not_found", language), register)
            } else "ytsearch:${this@loadYoutube}".loadYoutube(member, channel, register, musicPlaylist, true, consumer = consumer, lucky = lucky, speak = speak)
        }
    })
}


fun String.loadSpotifyTrack(member: Member, channel: TextChannel, register: ArdentRegister, musicPlaylist: DatabaseMusicPlaylist? = null, speak:Boolean=true,consumerFoundTrack: ((AudioTrack, String) -> Unit)? = null) {
    register.spotifyApi.tracks.getTrack(this.removePrefix("https://open.spotify.com/track/")).queue { track ->
        track?.let {
            "${it.name} ${it.artists.joinToString(", ") { artist -> artist.name }}".getSingleTrack(member, channel, { _, _, loaded ->
                consumerFoundTrack?.invoke(loaded, track.id)
                        ?: DEFAULT_TRACK_LOAD_HANDLER(member, channel, loaded, !speak, musicPlaylist, null, null, track.id, register)
            }, register, search = false, soundcloud = false)
        }
                ?: channel.send(register.translationManager.translate("music.specify_valid_spotify_track", member.guild.getLanguage(register)
                        ?: Language.ENGLISH), register)
    }
}

fun String.loadSpotifyAlbum(member: Member, channel: TextChannel, register: ArdentRegister, musicPlaylist: DatabaseMusicPlaylist? = null, consumerFoundTrack: ((AudioTrack, String) -> Unit)? = null) {
    val albumId = removePrefix("https://open.spotify.com/album/")
    val language = member.guild.getLanguage(register) ?: Language.ENGLISH
    register.spotifyApi.albums.getAlbum(albumId).queue { album ->
        if (album != null) {
             channel.send(register.translationManager.translate("music.beginning_spotify_album", language).apply(album.name), register)
            album.tracks.items.forEach { track ->
                "${track.name} ${track.artists[0].name}"
                        .getSingleTrack(member, channel, { _, _, loaded ->
                            consumerFoundTrack?.invoke(loaded, track.id)
                                    ?: DEFAULT_TRACK_LOAD_HANDLER(member, channel, loaded, true,
                                            musicPlaylist, null, album.id, track.id, register)
                        }, register, true)
            }
        } else channel.send(register.translationManager.translate("music.invalid_spotify_album", language), register)
    }
}

fun String.loadSpotifyPlaylist(member: Member, channel: TextChannel, register: ArdentRegister, musicPlaylist: DatabaseMusicPlaylist? = null, consumerFoundTrack: ((AudioTrack, String) -> Unit)? = null) {
    val language = member.guild.getLanguage(register) ?: Language.ENGLISH
    val split = removePrefix("https://open.spotify.com").split("/playlist/")
    val playlistId = split.getOrNull(1)
    if (playlistId == null) channel.send(register.translationManager.translate("music.invalid_spotify_playlist", language), register)
    else {
        register.spotifyApi.playlists.getPlaylist(playlistId).queue { playlist ->
            if (playlist != null) {
                channel.send(register.translationManager.translate("music.beginning_spotify_playlist", language).apply(playlist.name), register)
                playlist.tracks.items.forEach { track ->
                    "${track.track!!.name} ${track.track!!.artists[0].name}"
                            .getSingleTrack(member, channel, { _, _, loaded ->
                                consumerFoundTrack?.invoke(loaded, track.track!!.id)
                                        ?: DEFAULT_TRACK_LOAD_HANDLER(member, channel, loaded, true,
                                                musicPlaylist, playlist.id, null, track.track!!.id, register)
                            }, register, true)
                }
            } else channel.send(register.translationManager.translate("music.invalid_spotify_playlist", language), register)
        }
    }
}

fun String.getSingleTrack(member: Member, channel: TextChannel, foundConsumer: (Member, TextChannel, AudioTrack) -> (Unit), register: ArdentRegister, search: Boolean = false, soundcloud: Boolean = false) {
    val string = this
    playerManager.loadItemOrdered(member.guild.getAudioManager(null, register), "${if (search) (if (soundcloud) "scsearch:" else "ytsearch:") else ""}$this", object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            if (!soundcloud) string.getSingleTrack(member, channel, foundConsumer, register, true, search)
        }

        override fun trackLoaded(track: AudioTrack) {
            foundConsumer.invoke(member, channel, track)
        }

        override fun noMatches() {
            if (!search) {
                this@getSingleTrack.getSingleTrack(member, channel, foundConsumer, register, true, false)
            } else this@getSingleTrack.getSingleTrack(member, channel, foundConsumer, register, true, true)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult && playlist.tracks.size > 0) foundConsumer.invoke(member, channel, playlist.tracks[0])
        }
    })
}


/**
 * @return [Pair] with first as the title, and second as the video id
 */
fun String.searchYoutubeOfficialApi(register: ArdentRegister): List<Pair<String, String>>? {
    return try {
        val search = youtube.search().list("id,snippet")
        search.q = this
        search.key = register.config["google"]
        search.fields = "items(id/videoId,snippet/title)"
        search.maxResults = 7
        val response = search.execute()
        val items = response.items ?: return null
        items.filter { it != null }.map { Pair(it?.snippet?.title ?: "unavailable", it?.id?.videoId ?: "none") }
    } catch (e: Exception) {
        register.getTextChannel(register.config["error_channel"])!!.send("**Exception searching YouTube**:\n${ExceptionUtils.getStackTrace(e)}", register)
        null
    }
}