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
        when (arguments.getOrNull(0)) {
            "view" -> event.channel.send("You can view your music library at []".apply("$base/profile/${event.author.id}"), register)
            "add" -> {
                if (arguments.isEmpty()) {
                    event.channel.send("Please specify a song link or name", register)
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
            "reset" -> {
                event.channel.send(Emojis.INFORMATION_SOURCE.cmd + "Are you sure you want to reset your library? Type **yes** to continue", register)
                Sender.waitForMessage({ it.author.id == event.author.id && it.channel.id == event.channel.id && it.guild.id == event.guild.id }, {
                    if (it.message.contentRaw.startsWith("y", true)) {
                        val library = register.database.getMusicLibrary(event.author.id)
                        library.tracks = mutableListOf()
                        register.database.update(library)
                        event.channel.send(Emojis.BALLOT_BOX_WITH_CHECK.cmd + "Successfully reset your music library", register)
                    } else event.channel.send("**Yes** wasn't provided, so I cancelled the reset", register)
                })
            }
            "remove" -> event.channel.send("You can remove tracks at []. You need to sign in!".apply("$base/profile/${event.author.id}"), register)
            "play" -> {
                val library = register.database.getMusicLibrary(event.author.id)
                if (library.tracks.size == 0) event.channel.send(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                        "You don't have any tracks in your music library! Add some at [] or import your music library from **Spotify** with /mymusic import".apply("$base/profile/${event.author.id}")
                        , register)
                else {
                    event.channel.send("Started loading **[]** tracks from your music library..".apply(library.tracks.size), register)
                    library.load(event.member, event.channel, register)
                }
            }
            "import" -> {
                val url = register.spotifyApi.getAuthUrl(SpotifyClientAPI.Scope.USER_LIBRARY_READ, SpotifyClientAPI.Scope.PLAYLIST_READ_COLLABORATIVE,
                        SpotifyClientAPI.Scope.PLAYLIST_READ_PRIVATE, redirectUri = "$base/api/oauth/spotify") + "&state=${event.author.id}"
                event.author.openPrivateChannel().queue { channel ->
                    channel.sendMessage("To import your Spotify music library and add the tracks to your Ardent library, please login with your Spotify account using the following link: []"
                            .apply(url)).queue { message ->
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
                                        "Imported **[]** tracks from your Spotify library. View here: []"
                                                .apply(tracks.size, "$base/profile/${event.author.id}"), this, event)
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