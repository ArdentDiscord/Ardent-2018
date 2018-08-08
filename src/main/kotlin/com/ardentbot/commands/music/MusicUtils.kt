package com.ardentbot.commands.music

import com.adamratzman.spotify.utils.SimpleTrack
import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Sender
import com.ardentbot.core.managers
import com.ardentbot.core.playerManager
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteArray {
        return lastFrame!!.data
    }

    override fun isOpus(): Boolean {
        return true
    }
}

class GuildMusicManager(audioPlayerManager: AudioPlayerManager, var channel: TextChannel?, val guild: Guild, val register: ArdentRegister) {
    val player: AudioPlayer = audioPlayerManager.createPlayer()
    val scheduler: TrackScheduler = TrackScheduler(this, guild)
    val manager = ArdentMusicManager(player)
    internal val sendHandler: AudioPlayerSendHandler get() = AudioPlayerSendHandler(player)

    init {
        player.addListener(scheduler)
    }
}

class ArdentMusicManager(val player: AudioPlayer) {
    var queue = LinkedBlockingDeque<LocalTrackObj>()
    var current: LocalTrackObj? = null

    fun queue(track: LocalTrackObj) {
        if (!player.startTrack(track.track, true)) queue.offer(track)
        else current = track
    }

    fun skipToNextTrack() {
        val track = queue.poll()
        if (track?.track != null) {
            val set: Boolean = track.track!!.position != 0.toLong()
            try {
                player.startTrack(track.track, false)
            } catch (e: Exception) {
                player.startTrack(track.track!!.makeClone(), false)
            }
            if (set && player.playingTrack != null) player.playingTrack.position = track.track!!.position
            current = track
        } else {
            player.startTrack(null, false)
            current = null
        }
    }

    fun resetQueue() {
        this.queue = LinkedBlockingDeque()
    }

    fun addToBeginningOfQueue(track: LocalTrackObj) {
        track.track = track.track?.makeClone()
        if (track.track != null) queue.addFirst(track)
    }

    fun removeAt(num: Int): Boolean {
        val track = queue.toList().getOrNull(num) ?: return false
        queue.removeFirstOccurrence(track)
        return true
    }
}

class TrackScheduler(val manager: GuildMusicManager, val guild: Guild) : AudioEventAdapter() {
    var autoplay = true
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        autoplay = true
        Sender.scheduledExecutor.schedule({
            if (track.position == 0.toLong() && guild.selfMember.voiceState.inVoiceChannel() && !player.isPaused && player.playingTrack != null && player.playingTrack == track) {
                val queue = manager.manager.queue.toList()
                manager.player.isPaused = false
                manager.manager.resetQueue()
                val current = manager.manager.current
                manager.manager.skipToNextTrack()
                if (current != null) manager.manager.queue(current)
                queue.forEach { manager.manager.queue(it) }
            }
        }, 5, TimeUnit.SECONDS)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        try {
            if (guild.audioManager.isConnected) {
                manager.register.database.insert(LoggedTrack(guild.id, track.position / 1000.0 / 60.0 / 60.0))
                if (manager.manager.queue.size > 0) {
                    manager.manager.skipToNextTrack()
                } else if (player.playingTrack == null && manager.manager.queue.size == 0 && autoplay
                        && manager.register.database.getGuildMusicSettings(guild).autoplay && guild.selfMember.voiceState.channel != null
                        && guild.selfMember.voiceState.channel.members.size > 1) {
                    val spotifyApi = manager.register.spotifyApi
                    val current = manager.manager.current ?: return
                    try {
                        val recommendation: SimpleTrack? = when {
                            current.spotifyTrackId != null -> {
                                spotifyApi.tracks.getTrack(current.spotifyTrackId).complete()?.let { spotifyTrack ->
                                    spotifyApi.browse.getRecommendations(seedTracks = listOf(spotifyTrack.id), seedArtists = spotifyTrack.artists.map { it.id })
                                            .complete().tracks
                                }
                            }
                            current.spotifyAlbumId != null -> {
                                spotifyApi.albums.getAlbum(current.spotifyAlbumId).complete()?.let { album ->
                                    spotifyApi.browse.getRecommendations(seedArtists = album.artists.map { it.id }, seedGenres = album.genres)
                                            .complete().tracks
                                }
                            }
                            current.spotifyPlaylistId != null -> {
                                val split = current.spotifyPlaylistId.split(" :: ")
                                spotifyApi.playlists.getPlaylist(split[0], split[1]).complete()?.let { spotifyPlaylist ->
                                    spotifyApi.browse.getRecommendations(seedTracks = spotifyPlaylist.tracks.items.take(10).map { it.track.id })
                                            .complete().tracks
                                }
                            }
                            else -> {
                                val tempTrack = spotifyApi.search.searchTrack(
                                        track.info.title.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                                                .replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                                                + if (track.info.author.contains("official", true) || track.info.author.contains("vevo", true))
                                            track.info.author else "").complete().items.getOrNull(0)
                                tempTrack?.let { spotifyApi.browse.getRecommendations(seedTracks = listOf(tempTrack.id), seedArtists = tempTrack.artists.map { it.id }) }
                                        ?.complete()?.tracks
                            }
                        }?.getOrNull(0)
                        if (recommendation != null) {
                            val channel = manager.channel ?: guild.defaultChannel
                            if (channel != null) {
                                "https://open.spotify.com/track/${recommendation.id}"
                                        .loadSpotifyTrack(guild.selfMember, channel, consumerFoundTrack = { audioTrack, _ ->
                                            channel.send("${Emojis.BALLOT_BOX_WITH_CHECK} " + "**[Autoplay]** "
                                                    + "Adding **[]** by **[]** to the queue *[]*...".apply(recommendation.name, recommendation.artists.joinToString { it.name },
                                                    audioTrack.getDurationString()), manager.register)
                                            play(channel, guild.selfMember, LocalTrackObj(guild.selfMember.user.id, guild.selfMember.user.id, null,
                                                    null, null, recommendation.id, audioTrack), manager.register)
                                        }, register = manager.register)
                                return
                            }
                        } else manager.channel?.send("Couldn't find this song in the Spotify database, no autoplay available.", manager.register)
                    }
                    catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            manager.register.getTextChannel("419283618976759809")!!.sendMessage(ExceptionUtils.getStackTrace(e)).queue()
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        manager.manager.skipToNextTrack()
        manager.channel?.send("${Emojis.BALLOT_BOX_WITH_CHECK} " + "Oh no! My voice connection got stuck (#blamediscord) - I'll attempt to skip now now - If you encounter this repeatedly, please make me leave then rejoin the channel!",
                manager.register)
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        onException(exception)
    }

    private fun onException(exception: FriendlyException) {
        manager.manager.current = null
        manager.manager.skipToNextTrack()
        manager.channel?.send("I couldn't play that track, sorry :( - Reason: *[]*".apply(exception.localizedMessage), manager.register)
    }
}

fun AudioTrack.getDurationString(): String {
    return info.length.getDurationString()
}

fun Long.getDurationString(): String {
    val seconds = (this / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60
    return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String.format("%02d", seconds % 60)}]"
}

fun AudioTrack.getCurrentTime(): String {
    val current = position
    val seconds = (current / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60

    val length = info.length
    val lengthSeconds = (length / 1000).toInt()
    val lengthMinutes = lengthSeconds / 60
    val lengthHours = lengthMinutes / 60

    return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String
            .format("%02d", seconds % 60)} / ${String.format("%02d", lengthHours % 60)}:${String
            .format("%02d", lengthMinutes % 60)}:${String.format("%02d", lengthSeconds % 60)}]"
}

@Synchronized
fun Guild.getAudioManager(channel: TextChannel?, register: ArdentRegister): GuildMusicManager {
    val guildId = id.toLong()
    var musicManager = managers[guildId]
    if (musicManager == null) {
        musicManager = GuildMusicManager(playerManager, channel, this, register)
        audioManager.sendingHandler = musicManager.sendHandler
        managers[guildId] = musicManager
    } else if (channel != null) musicManager.channel = channel
    return musicManager
}

fun VoiceChannel.connect(textChannel: TextChannel?, register: ArdentRegister, complain: Boolean = true): Boolean {
    val audioManager = guild.audioManager
    return try {
        audioManager.openAudioConnection(this)
        true
    } catch (e: Throwable) {
        if (complain) textChannel?.send("${Emojis.CROSS_MARK} " + "I can't join the **[]** voice channel! Reason: *[]*".apply(name, e.localizedMessage),
                register)
        false
    }
}

fun play(channel: TextChannel?, member: Member, track: LocalTrackObj, register: ArdentRegister) {
    if (member.voiceState.channel != null) member.voiceState.channel.connect(channel, register)
    else {
        channel?.send("Unable to join voice channel.. Are you sure you're in one?", register)
        return
    }
    member.guild.getAudioManager(channel, register).manager.queue(track)
}


fun List<LocalTrackObj>.toTrackDisplay(): List<TrackDisplay> {
    val display = mutableListOf<TrackDisplay>()
    forEach { if (it.track != null) display.add(TrackDisplay(it.track!!.info.title, it.track!!.info.author)) }
    return display
}

fun LocalTrackObj.getInfo(guild: Guild, register: ArdentRegister, curr: Boolean = false): String {
    return "**[]** by *[]* [] - added by **[]**"
            .apply(track!!.info.title, track!!.info.author, if (curr) track!!.getCurrentTime() else track!!.getDurationString(), register.getUser(user)?.display()
                    ?: "Unknown")
}
