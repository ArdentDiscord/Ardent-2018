package com.ardentbot.commands.music.playback

import com.ardentbot.commands.games.send
import com.ardentbot.commands.music.DatabaseTrackObj
import com.ardentbot.commands.music.getAudioManager
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("music")
class Save : Command("save", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val manager = event.guild.getAudioManager(event.channel, register)
        val player = manager.player
        if (player.playingTrack == null) event.channel.send("There isn't a playing track!", register)
        else {
            val track = manager.manager.current!!
            val library = register.database.getMusicLibrary(event.author.id)
            library.tracks.add(DatabaseTrackObj(event.author.id, System.currentTimeMillis(), track.playlist?.playlist?.id as? String,
                    track.track!!.info.title, track.track!!.info.author, track.getUri()!!))
            library.lastModified = System.currentTimeMillis()
            register.database.update(library)
            event.channel.send(Emojis.WHITE_HEAVY_CHECKMARK.cmd + "Updated your music library. Type **/mymusic** for more information",register)
        }
    }
}