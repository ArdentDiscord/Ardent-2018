package com.ardentbot.core

import com.adamratzman.spotify.SpotifyApi
import com.ardentbot.commands.games.send
import com.ardentbot.commands.info.StatusData
import com.ardentbot.commands.music.*
import com.ardentbot.core.commands.CommandHolder
import com.ardentbot.core.database.Database
import com.ardentbot.core.translation.TranslationManager
import com.ardentbot.core.utils.Config
import com.ardentbot.core.utils.ErrorService
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.web.Web
import com.ardentbot.web.base
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.factory.DefaultSendFactory
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.hooks.AnnotatedEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val playerManager = DefaultAudioPlayerManager()
val managers = ConcurrentHashMap<Long, GuildMusicManager>()

fun main(args: Array<String>) {
    ArdentRegister(args)
}

class ArdentRegister(args: Array<String>) {
    val random = Random()
    val test = args[2].toBoolean()
    val cachedExecutor = Executors.newCachedThreadPool()
    val config = Config(args[0], args.toList())
    val processor = Processor(this)
    val jda = JDABuilder.create(config["token"], GatewayIntent.values().toList())
            .setEventManager(AnnotatedEventManager())
            .addEventListeners(processor)
            .setAudioSendFactory(if (test) DefaultSendFactory() else NativeAudioSendFactory())
            .build().awaitReady()
    val selfUser = jda.selfUser
    val translationManager = TranslationManager(this)
    val database = Database(this)
    val parser = Parser()
    val errorService = ErrorService(this)
    val sender = Sender(this)
    val holder = CommandHolder(this)
    val web = Web(this)
    val spotifyApi = SpotifyApi.spotifyAppApi(config["spotify_client_id"], config["spotify_client_secret"]).build()
    val cmdChannel = jda.getTextChannelById("363785648911679488")
    fun getTextChannel(id: String): TextChannel? {
        return try {
            jda.getTextChannelById(id)
        } catch (e: Exception) {
            null
        }
    }

    fun getGuild(id: String): Guild? {
        return try {
            jda.getGuildById(id)
        } catch (e: Exception) {
            null
        }

    }

    fun getUser(id: String): User? {
        return try {
            jda.getUserById(id)
        } catch (e: Exception) {
            null
        }
    }

    fun getVoiceChannel(id: String): VoiceChannel? {
        return try {
            jda.getVoiceChannelById(id)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllGuilds() = jda.guilds
    fun getAllUsers() = jda.users
    fun getApiCalls() = jda.responseTotal
    fun getMutualGuildsWith(user: User): List<Guild> {
        return getAllGuilds().filter { guild -> guild.members.map { it.user.id }.contains(user.id) }
    }

    init {
     //   playerManager.useRemoteNodes("lavaplayer1:8080", "lavaplayer2:8080")
        playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
        playerManager.registerSourceManager(YoutubeAudioSourceManager())
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
        playerManager.registerSourceManager(HttpAudioSourceManager())
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)

        // resume playback after restarts
        Sender.scheduledExecutor.scheduleAtFixedRate({ checkQueueBackups() }, 0, 30, TimeUnit.SECONDS)

        // disconnect from voice channels where stayinchannel=false and channel members size = 1
        Sender.scheduledExecutor.scheduleAtFixedRate({ checkVoiceChannels() }, 30, 30, TimeUnit.SECONDS)

        // checking for expired mutes and announcements
        Sender.scheduledExecutor.scheduleAtFixedRate({
            database.getAnnouncements().forEach { announcement ->
                if (announcement.lastAnnouncementTime == null && System.currentTimeMillis() >= announcement.startTime) {
                    val guild = getGuild(announcement.guildId)
                    val channel = guild?.getTextChannelById(announcement.channel)
                    if (channel == null) guild?.owner?.user?.openPrivateChannel()?.queue {
                        it.sendMessage(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                "I was unable to announce ```[]``` in **[]** because the channel provided is invalid."
                                        .apply(announcement.message, guild.name) + " " + "Canceled announcement..").queue()
                    } else {
                        channel.send(announcement.getEditedMessage(), this)
                        if (announcement.timeBetweenRepetitions == null) database.delete(announcement)
                        else {
                            announcement.lastAnnouncementTime = System.currentTimeMillis()
                            database.update(announcement)
                        }
                    }
                } else if (announcement.lastAnnouncementTime != null && (announcement.repetitionAmountLeft == null
                                || announcement.repetitionAmountLeft!! > 0)) {
                    val next = announcement.lastAnnouncementTime!! + announcement.timeBetweenRepetitions!!
                    if (System.currentTimeMillis() >= next) {
                        if (announcement.repetitionAmountLeft != null) {
                            announcement.repetitionAmountLeft = announcement.repetitionAmountLeft!! - 1
                        }

                        val guild = getGuild(announcement.guildId)
                        val channel = guild?.getTextChannelById(announcement.channel)
                        if (channel == null) guild?.owner?.user?.openPrivateChannel()?.queue {
                            it.sendMessage(Emojis.HEAVY_MULTIPLICATION_X.cmd +
                                    "I was unable to announce ```[]``` in **[]** because the channel provided is invalid."
                                            .apply(announcement.message, guild.name) + " " + "Canceled announcement..").queue()
                        } else {
                            channel.send(announcement.getEditedMessage(), this)
                            if (announcement.repetitionAmountLeft == 0) database.delete(announcement)
                            else {
                                announcement.lastAnnouncementTime = System.currentTimeMillis()
                                database.update(announcement)
                            }
                        }
                    }
                }
            }

            database.getMutes().forEach { mute ->
                val guild = getGuild(mute.guildId)
                if (guild?.getMemberById(mute.muted) != null) {
                    val data = database.getGuildData(guild)
                    val member = guild.getMemberById(mute.muted)!!
                    if (member.roles.map { it.id }.contains(data.muteRoleId)) {
                        if (System.currentTimeMillis() >= mute.expiresAt) {
                            try {
                                guild.removeRoleFromMember(member, member.roles.first { it.id == data.muteRoleId })
                                        .reason("Unmuted").queue {
                                            member.user.openPrivateChannel().queue { channel ->
                                                sender.send("You've been unmuted in **[]** []"
                                                        .apply(guild.name, Emojis.SLIGHTLY_SMILING_FACE.symbol),
                                                        null, channel, member.user, null)
                                            }
                                        }
                                database.delete(mute, blocking = false)
                            } catch (e: Exception) {
                            }
                        }
                    } else database.delete(mute, blocking = false)
                } else database.delete(mute, blocking = false)
            }
        }, 0, 30, TimeUnit.SECONDS)

        Sender.scheduledExecutor.scheduleWithFixedDelay({
            val users = database.getStatusChangeUsers().toList().map { it.toLong() }
            getAllGuilds().map { guild -> guild.members.map { it.user to it.onlineStatus } }.flatten()
                    .filterNot { it.first.idLong in users }.forEachIndexed { i, it ->
                        if (i % 500 == 0) println("Inserted batch of 500 ($i) into statuses")
                        database.insert(StatusData(it.first.id, 0, 0, 0, 0, it.second, System.currentTimeMillis()))
                    }
        }, 0, 15, TimeUnit.MINUTES)

        Sender.scheduledExecutor.scheduleAtFixedRate({
            jda.presence.activity = Activity.playing(when(random.nextInt(7)) {
                0 -> "/help | With ${holder.commands.size} commands!"
                1 -> "Share Ardent with friends! | /help"
                2 -> "/music | Music streaming"
                3 -> "In ${getAllGuilds().size} servers! | /help"
                4 -> "ardentbot.com/invite"
                5 -> "ardentbot.com/invite"
                else -> "Use /help"
            })
        },0,15,TimeUnit.SECONDS)

        println("Ardent has started ${Emojis.SMILING_FACE_WITH_SUN_GLASS.symbol}")
    }

    private fun checkQueueBackups() {
        val queues = database.getSavedQueues()
        queues.forEach { queue ->
            if (queue.tracks.isEmpty()) return
            val channel = getVoiceChannel(queue.voiceId) ?: return
            val textChannel = queue.channelId?.let { getTextChannel(queue.channelId) } ?: return
            if (channel.members.size > 1 || (channel.members.size == 1 && channel.members[0] == channel.guild.selfMember)) {
                val manager = channel.guild.getAudioManager(textChannel, this)
                if (manager.channel != null) {
                    if (channel.guild.selfMember.voiceState?.channel != channel) channel.connect(textChannel, this)
                    textChannel.send(("**Restarting playback...**... Check out [] for other cool features we offer in Ardent **Premium**").apply("<$base/premium>"), this)
                    queue.tracks.forEach { trackUrl ->
                        trackUrl.load(channel.guild.selfMember, textChannel, this) { audioTrack, id ->
                            play(manager.channel, channel.guild.selfMember, LocalTrackObj(channel.guild.selfMember.user.id, channel.guild.selfMember.user.id, null, null, null, id, audioTrack), this)
                        }
                    }
                }
            }
        }
        database.delete("saved_queues")
    }

    private fun checkVoiceChannels() {
        managers.filter {
            it.value.guild.audioManager.connectedChannel != null && !database.getGuildMusicSettings(it.value.guild).stayInChannel
                    && it.value.guild.audioManager.connectedChannel!!.members.filterNot { member -> member.user.isBot }.count() == 0
        }.forEach { (idLong, manager) ->
            manager.player.destroy()
            managers.remove(idLong)
            val name = manager.guild.audioManager.connectedChannel!!.name
            manager.guild.audioManager.closeAudioConnection()
            manager.scheduler.autoplay = false
            manager.channel?.send("Left **[]** because I was the only one in the channel! You can change this setting at []"
                    .apply(name, "<$base/manage/${manager.guild.id}>"), this)
        }
    }
}

fun String.toUser(register: ArdentRegister) = register.getUser(this)