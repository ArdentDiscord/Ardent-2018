package com.ardentbot.commands.music.management

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.DatabaseMusicPlaylist
import com.ardentbot.commands.music.getPlaylistById
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.ArgumentInformation
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.asPojo
import com.ardentbot.core.database.genId
import com.ardentbot.core.selectFromList
import com.ardentbot.kotlin.*
import com.ardentbot.web.base
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.stream.Collectors

@ModuleMapping("music")
class Playlists : Command("playlist", arrayOf("playlists"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "play" -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send("You need to specify a valid playlist id!", register)
                else {
                    event.channel.send("Loading tracks from playlist **[]**..".apply(playlist.name), register)
                    playlist.toLocalPlaylist(event.member).loadTracks(event.channel, event.member, register)
                }
            }
            "list" -> {
                val user = event.message.mentionedUsers.getOrElse(0) { event.author }
                val embed = getEmbed("[] | Music Playlists".apply(user.display()), event.channel)
                val playlists = register.database.getPlaylists(user.id)
                if (playlists.isEmpty()) embed.appendDescription("This user doesn't have any playlists! Create one by typing */playlist create [name]*")
                else {
                    playlists.forEachIndexed { index, playlist ->
                        embed.appendDescription(index.diamond() + " " +
                                "[**[]**]($base/music/playlist/[]) - ID: *[]* - Last Modified at *[]*"
                                        .apply(playlist.name, playlist.id, playlist.id, playlist.lastModified.localeDate()) + "\n\n")
                    }
                    embed.appendDescription("You can play a playlist using */playlist play [playlist id]*")
                }
                event.channel.send(embed, register)
            }
            "view" -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send("You need to specify a valid playlist id!", register)
                else {
                    event.channel.send("To see track information for, or modify **[]** *by []*, go to [] - id: *[]*"
                            .apply(playlist.name, register.getUser(playlist.owner)?.display()
                                    ?: "Unknown", "https://ardentbot.com/music/playlist/${playlist.id}", playlist.id), register)
                }
            }
            "delete" -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send("You need to specify a valid playlist id!", register)
                else {
                    if (playlist.owner != event.author.id) event.channel.send("You need to be the owner of this playlist in order to delete it!", register)
                    else {
                        event.channel.selectFromList(event.member, "Are you sure you want to delete the playlist []? This is irreversible"
                                .apply(playlist.name), mutableListOf("Yes", "No"), { selection, m ->
                            if (selection == 0) {
                                register.database.delete(playlist)
                                event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Deleted the playlist **[]**".apply(playlist.name), register)
                            } else event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.symbol + " " + "Cancelled playlist deletion..", register)
                            m.delete()
                        }, register = register)
                    }
                }
            }
            "create" -> {
                if (arguments.size == 1) event.channel.send("You need to include a name for this playlist", register)
                else {
                    val name = arguments.without(0).concat()
                    event.channel.selectFromList(event.member, "What type of playlist do you want to create?",
                            mutableListOf("Default", "Link a Spotify Playlist or Album", "Clone someone's playlist", "Link a YouTube Playlist"), { selection, msg ->
                        msg.delete().queue()
                        when (selection) {
                            0 -> {
                                event.channel.send("Successfully created the playlist **[]**!".apply(name), register)
                                val playlist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                        null, null, null, tracks = mutableListOf())
                                register.database.insert(playlist)
                                event.channel.send("View this playlist online at []".apply("$base/music/playlist/${playlist.id}"), register)
                            }
                            1 -> {
                                event.channel.send("Please enter in a Spotify playlist or album url now", register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw.split("?")[0]
                                    val playlist: DatabaseMusicPlaylist? = when {
                                        url.startsWith("https://open.spotify.com/album/") -> {
                                            event.channel.send("Successfully created the playlist **[]**!".apply(name), register)
                                            DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                    url.removePrefix("https://open.spotify.com/album/"), null, null)
                                        }
                                        url.startsWith("https://open.spotify.com/user/") -> {
                                            event.channel.send("Successfully created the playlist **[]**!".apply(name), register)
                                            DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                    null, url.removePrefix("https://open.spotify.com/user/")
                                                    .split("/playlist/").stream().collect(Collectors.joining("||")), null)
                                        }
                                        else -> {
                                            event.channel.send("You specified an invalid url. Cancelled playlist setup.", register)
                                            null
                                        }
                                    }
                                    if (playlist != null) {
                                        register.database.insert(playlist)
                                        event.channel.send("View this playlist online at []".apply("$base/music/playlist/${playlist.id}"), register)
                                    }
                                })
                            }
                            2 -> {
                                event.channel.send("Please enter in an Ardent playlist id or url", register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw.replace("https://ardentbot.com/music/playlist/", "")
                                    val playlist = getPlaylistById(url, register)
                                    if (playlist == null) event.channel.send("You specified an invalid playlist. Please try again", register)
                                    else {
                                        val newPlaylist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                playlist.spotifyAlbumId, playlist.spotifyPlaylistId, playlist.youtubePlaylistUrl, playlist.tracks)
                                        register.database.insert(newPlaylist)
                                        event.channel.send("Successfully cloned **[]**!".apply(playlist.name), register)
                                        event.channel.send("View this playlist online at []".apply("$base/music/playlist/${newPlaylist.id}"), register)
                                    }
                                })
                            }
                            else -> {
                                event.channel.send("Please specify a YouTube playlist url now.", register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw
                                    if (url.startsWith("https://www.youtube.com/playlist?list=") || url.startsWith("https://youtube.com/playlist?list=")) {
                                        event.channel.send("Successfully created the playlist **[]**!".apply(name), register)
                                        val playlist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                null, null, url, tracks = mutableListOf())
                                        register.database.insert(playlist)
                                        event.channel.send("View this playlist online at []".apply("$base/music/playlist/${playlist.id}"), register)
                                    } else {
                                        event.channel.send("You specified an invalid url. Cancelled playlist setup.", register)
                                    }
                                })
                            }
                        }
                    }, failure = {
                        event.channel.send("Cancelled playlist creation.", register)
                    }, register = register)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val play = ArgumentInformation("play [playlist id]", "start playback of a playlist")
    val list = ArgumentInformation("list @User", "list a user's playlists")
    val view = ArgumentInformation("view [playlist id]", "get an overview of the specified playlist")
    val delete = ArgumentInformation("delete [playlist id]", "delete a playlist")
    val create = ArgumentInformation("create", "create a new playlist")
}