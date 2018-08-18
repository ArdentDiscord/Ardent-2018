package com.ardentbot.commands.music.management

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.DatabaseMusicPlaylist
import com.ardentbot.commands.music.getPlaylistById
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Argument
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
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("play", event.guild, register) == true -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send(translate("playlists.specify_valid_id", event, register), register)
                else {
                    event.channel.send(translate("playlists.loading_tracks", event, register).apply("**${playlist.name}**"), register)
                    playlist.toLocalPlaylist(event.member).loadTracks(event.channel, event.member, register)
                }
            }
            arg?.isTranslatedArgument("list", event.guild, register) == true -> {
                val user = event.message.mentionedUsers.getOrElse(0) { event.author }
                val embed = getEmbed(translate("playlists.list_embed_title", event, register).apply(user.display()), event.channel)
                val playlists = register.database.getPlaylists(user.id)
                if (playlists.isEmpty()) embed.appendDescription(translate("playlists.no_playlists", event, register))
                else {
                    playlists.forEachIndexed { index, playlist ->
                        embed.appendDescription(index.diamond() +
                                "[**[]**]($base/music/playlist/[]) - " + translate("playlists.list_row", event, register)
                                .apply(playlist.name, playlist.id, playlist.id, playlist.lastModified.localeDate()) + "\n\n")
                    }
                    embed.appendDescription(translate("playlists.how_play", event, register))
                }
                event.channel.send(embed, register)
            }
            arg?.isTranslatedArgument("view", event.guild, register) == true -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send(translate("playlists.specify_valid_id", event, register), register)
                else {
                    event.channel.send(translate("playlists.view_response", event, register)
                            .apply(playlist.name, register.getUser(playlist.owner)?.display()
                                    ?: translate("unknown", event, register), "https://ardentbot.com/music/playlist/${playlist.id}", playlist.id), register)
                }
            }
            arg?.isTranslatedArgument("delete", event.guild, register) == true -> {
                val playlist: DatabaseMusicPlaylist? = arguments.getOrNull(1)?.let {
                    asPojo(register.database.get("music_playlists", it) as HashMap<*, *>?, DatabaseMusicPlaylist::class.java)
                }
                if (playlist == null) event.channel.send(translate("playlists.specify_valid_id", event, register), register)
                else {
                    if (playlist.owner != event.author.id) event.channel.send(translate("playlists.delete_permission", event, register), register)
                    else {
                        event.channel.selectFromList(event.member, translate("playlists.confirm_delete", event, register)
                                .apply("**${playlist.name}**"), mutableListOf(translate("yes", event, register),
                                translate("no", event, register)), { selection, m ->
                            if (selection == 0) {
                                register.database.delete(playlist)
                                event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd +
                                        translate("playlists.delete_success", event, register).apply("**${playlist.name}**"), register)
                            } else event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd + translate("playlists.cancel_deletion", event, register), register)
                            m.delete()
                        }, register = register)
                    }
                }
            }
            arg?.isTranslatedArgument("create", event.guild, register) == true -> {
                if (arguments.size == 1) event.channel.send(translate("playlists.create_need_name", event, register), register)
                else {
                    val name = arguments.without(0).concat()
                    event.channel.selectFromList(event.member, translate("playlists.create_what_type", event, register),
                            mutableListOf(translate("playlists.default", event, register), translate("playlists.link_spotify", event, register),
                                    translate("playlists.clone_other", event, register), translate("playlists.link_youtube", event, register)), { selection, msg ->
                        msg.delete().queue()
                        when (selection) {
                            0 -> {
                                event.channel.send(translate("playlists.create_success", event, register).apply("**$name**"), register)
                                val playlist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                        null, null, null, tracks = mutableListOf())
                                register.database.insert(playlist)
                                event.channel.send(translate("playlists.view_this_online", event, register).apply("$base/music/playlist/${playlist.id}"), register)
                            }
                            1 -> {
                                event.channel.send(translate("playlists.specify_spotify_now", event, register), register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw.split("?")[0]
                                    val playlist: DatabaseMusicPlaylist? = when {
                                        url.startsWith("https://open.spotify.com/album/") -> {
                                            event.channel.send(translate("playlists.create_success", event, register).apply("**$name**"), register)
                                            DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                    url.removePrefix("https://open.spotify.com/album/"), null, null)
                                        }
                                        url.startsWith("https://open.spotify.com/user/") -> {
                                            event.channel.send(translate("playlists.create_success", event, register).apply("**$name**"), register)
                                            DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                    null, url.removePrefix("https://open.spotify.com/user/")
                                                    .split("/playlist/").stream().collect(Collectors.joining("||")), null)
                                        }
                                        else -> {
                                            event.channel.send(translate("playlists.invalid_url_try_again", event, register), register)
                                            null
                                        }
                                    }
                                    if (playlist != null) {
                                        register.database.insert(playlist)
                                        event.channel.send(translate("playlists.view_this_online", event, register).apply("$base/music/playlist/${playlist.id}"), register)
                                    }
                                })
                            }
                            2 -> {
                                event.channel.send(translate("playlists.specify_ardent_playlist", event, register), register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw.replace("https://ardentbot.com/music/playlist/", "")
                                    val playlist = getPlaylistById(url, register)
                                    if (playlist == null) event.channel.send(translate("playlists.invalid_playlist_try_again", event, register), register)
                                    else {
                                        val newPlaylist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                playlist.spotifyAlbumId, playlist.spotifyPlaylistId, playlist.youtubePlaylistUrl, playlist.tracks)
                                        register.database.insert(newPlaylist)
                                        event.channel.send(translate("playlists.clone_success", event, register).apply("**${playlist.name}**"), register)
                                        event.channel.send(translate("playlists.view_this_online", event, register).apply("$base/music/playlist/${newPlaylist.id}"), register)
                                    }
                                })
                            }
                            else -> {
                                event.channel.send(translate("playlists.specify_youtube_playlist", event, register), register)
                                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, { reply ->
                                    val url = reply.message.contentRaw
                                    if (url.startsWith("https://www.youtube.com/playlist?list=") || url.startsWith("https://youtube.com/playlist?list=")) {
                                        event.channel.send(translate("playlists.create_success", event, register).apply("**$name**"), register)
                                        val playlist = DatabaseMusicPlaylist(genId(6, "music_playlists"), event.author.id, name, System.currentTimeMillis(),
                                                null, null, url, tracks = mutableListOf())
                                        register.database.insert(playlist)
                                        event.channel.send(translate("playlists.view_this_online", event, register).apply("$base/music/playlist/${playlist.id}"), register)
                                    } else {
                                        event.channel.send(translate("playlists.create_invalid_url", event, register), register)
                                    }
                                })
                            }
                        }
                    }, failure = {
                        event.channel.send(translate("playlists.create_cancel", event, register), register)
                    }, register = register)
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val play = Argument("play")
    val list = Argument("list")
    val view = Argument("view")
    val delete = Argument("delete")
    val create = Argument("create")
}