package com.ardentbot.commands.music.management

import com.adamratzman.spotify.main.SpotifyClientAPI
import com.adamratzman.spotify.utils.SavedTrack
import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.DatabaseTrackObj
import com.ardentbot.commands.music.loadExternally
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.ExternalAction
import com.ardentbot.core.Flag
import com.ardentbot.core.Sender
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import com.ardentbot.web.base
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class MusicLibrary : Command("musiclibrary", arrayOf("library", "mymusic"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val arg = arguments.getOrNull(0)
        when {
            arg?.isTranslatedArgument("view", event.guild, register) == true -> {
                event.channel.send(translate("musiclibrary.view_at", event, register).apply("$base/profile/${event.author.id}"), register)
            }
            arg?.isTranslatedArgument("add", event.guild, register) == true -> {
                if (arguments.isEmpty()) {
                    event.channel.send(translate("music.specify_song", event, register), register)
                } else {
                    val library = register.database.getMusicLibrary(event.author.id)
                    arguments.concat().loadExternally(register) { audioTrack, _ ->
                        library.lastModified = System.currentTimeMillis()
                        library.tracks.add(DatabaseTrackObj(event.author.id, System.currentTimeMillis(), null, audioTrack.info.title,
                                audioTrack.info.author, if (arguments.concat().startsWith("http")) arguments.concat() else audioTrack.info.uri))
                        register.database.update(library)
                    }
                }
            }
            arg?.isTranslatedArgument("reset", event.guild, register) == true -> {
                event.channel.send(Emojis.INFORMATION_SOURCE.cmd + translate("musiclibrary.reset_warning", event, register), register)
                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, {
                    if (it.message.contentRaw.startsWith("y", true) || it.message.contentRaw.isTranslatedPhrase("yes", event.guild, register)) {
                        val library = register.database.getMusicLibrary(event.author.id)
                        library.tracks = mutableListOf()
                        register.database.update(library)
                        event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd + translate("musiclibrary.reset_success", event, register), register)
                    } else event.channel.send(translate("musiclibrary.reset_cancel", event, register), register)
                })
            }
            arg?.isTranslatedArgument("remove", event.guild, register) == true -> {
                event.channel.send(translate("musiclibrary.how_remove", event, register).apply("$base/profile/${event.author.id}"), register)
            }
            arg?.isTranslatedArgument("play", event.guild, register) == true -> {
                val library = register.database.getMusicLibrary(event.author.id)
                if (library.tracks.size == 0) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        translate("musiclibrary.how_add_import", event, register).apply("$base/profile/${event.author.id}")
                        , register)
                else {
                    event.channel.send(translate("musiclibrary.loading", event, register).apply(library.tracks.size), register)
                    library.load(event.member, event.channel, register)
                }
            }
            arg?.isTranslatedArgument("import", event.guild, register) == true -> {
                val url = register.spotifyApi.getAuthUrl(SpotifyClientAPI.Scope.USER_LIBRARY_READ, SpotifyClientAPI.Scope.PLAYLIST_READ_COLLABORATIVE,
                        SpotifyClientAPI.Scope.PLAYLIST_READ_PRIVATE, redirectUri = "$base/api/oauth/spotify") + "&state=${event.author.id}"
                event.author.openPrivateChannel().queue { channel ->
                    channel.sendMessage(translate("musiclibrary.import_how", event, register).apply(url)).queue { message ->
                        ExternalAction.waitSpotify(event.member, event.channel, register) { any ->
                            message.delete().queue()
                            val client = any as SpotifyClientAPI
                            val library = register.database.getMusicLibrary(event.author.id)
                            client.clientLibrary.getSavedTracks(50).queue { savedTracks ->
                                val tracks = savedTracks.items.toMutableList()
                                var curr = savedTracks
                                while (curr.next != null) {
                                    curr = curr.getNext<SavedTrack>().complete()
                                    tracks.addAll(curr.items)
                                }

                                tracks.forEach { track ->
                                    library.tracks.add(DatabaseTrackObj(event.author.id, System.currentTimeMillis(), null,
                                            track.track.name, track.track.artists.joinToString { it.name }, track.track.external_urls["spotify"]!!))
                                }

                                register.database.update(library)
                                register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd +
                                        translate("musiclibrary.successful_import", event, register).apply(tracks.size,
                                                "$base/profile/${event.author.id}"), this, event)
                            }
                        }
                    }
                }
            }
            else -> displayHelp(event, arguments, flags, register)
        }
    }

    val view = Argument("view")
    val play = Argument("play")
    val add = Argument("add")
    val remove = Argument("remove")
    val reset = Argument("reset")
    val import = Argument("import")
}