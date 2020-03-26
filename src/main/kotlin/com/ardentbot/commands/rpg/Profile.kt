package com.ardentbot.commands.rpg

import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyScope
import com.ardentbot.core.*
import com.ardentbot.core.commands.Argument
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.core.database.getUserData
import com.ardentbot.kotlin.*
import com.ardentbot.web.base
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("rpg")
class Profile : Command("profile", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        when (arguments.getOrNull(0)) {
            "help" -> {
                val embed = getEmbed("Change your profile | Help", event.author, event.guild)
                val options = listOf(
                        "name [your name]" to "change the name displayed in your profile",
                        "from [country, state, or place]" to "set where you're from",
                        "spotify" to "link your Spotify account to Ardent!",
                        "languages add [language]" to "add a language you speak to your profile",
                        "languages remove [language]" to "remove a language from your profile",
                        "about [about you!]" to "set the `About me` section on your profile",
                        "remove [name, from, languages, about, spotify]" to "remove the specified section on your profile")
                        .map { "/profile **" + it.first + "**:" + "*" + it.second + "*" }
                        .embedify()
                embed.setDescription(options)
                        .appendDescription("\n\n")
                        .appendDescription("__Example__: /profile remove languages")
                register.sender.cmdSend(embed, this, event)
            }
            "name" -> {
                if (arguments.size == 1) onInvoke(event, listOf("help"), flags, register)
                else {
                    val data = register.database.getUserData(event.author.id)
                    data.name = arguments.without(0).concat()
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile: **[]**".apply("Name"), this, event)
                }
            }
            "from" -> {
                if (arguments.size == 1) onInvoke(event, listOf("help"), flags, register)
                else {
                    val data = register.database.getUserData(event.author.id)
                    data.from = arguments.without(0).concat()
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile: **[]**".apply("From"), this, event)
                }
            }
            "spotify" -> {
                val url = register.spotifyApi.getAuthorizationUrl(SpotifyScope.USER_LIBRARY_READ, SpotifyScope.PLAYLIST_READ_COLLABORATIVE,
                        SpotifyScope.PLAYLIST_READ_PRIVATE, redirectUri = "$base/api/oauth/spotify") + "&state=${event.author.id}"
                event.author.openPrivateChannel().queue {
                    it.sendMessage("Connect your Spotify account using the following link: []".apply(url)).queue { message ->
                        ExternalAction.waitSpotify(event.member!!, event.channel, register) { client ->
                            message.delete().queue()
                            client as SpotifyClientApi
                            val data = register.database.getUserData(event.author)
                            client.users.getClientProfile().queue {
                                data.spotifyId = it.id
                                register.database.update(data)
                                register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Registered Spotify account: **[]**"
                                        .apply(it.displayName ?: it.id), this, event)
                            }
                        }
                    }
                }
            }
            "language", "languages" -> {
                when (arguments.getOrNull(1)) {
                    "add" -> {
                        if (arguments.size == 2) onInvoke(event, listOf("help"), flags, register)
                        else {
                            val data = register.database.getUserData(event.author.id)
                            if (data.languages == null) data.languages = mutableListOf()
                            data.languages!!.add(arguments.without(0).without(0).concat())
                            register.database.update(data)
                            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile: **[]**".apply("Speaks"), this, event)
                        }
                    }
                    "remove" -> {
                        if (arguments.size == 1) onInvoke(event, listOf("help"), flags, register)
                        else {
                            val query = arguments.without(0).without(0).concat()
                            val data = register.database.getUserData(event.author.id)
                            if (data.languages?.contains(query) == true) {
                                if (data.languages!!.size == 1) data.languages = null
                                else data.languages!!.remove(query)
                                register.database.update(data)
                                register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile: **[]**".apply("Speaks"), this, event)
                            } else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + "Language **[]** not found"
                                    .apply(query), this, event)
                        }
                    }
                }
            }
            "about" -> {
                if (arguments.size == 1) onInvoke(event, listOf("help"), flags, register)
                else {
                    val data = register.database.getUserData(event.author.id)
                    data.aboutMe = arguments.without(0).concat()
                    register.database.update(data)
                    register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile: **[]**".apply("About me"), this, event)
                }
            }
            "remove" -> {
                val data = register.database.getUserData(event.author.id)
                when (arguments.getOrNull(1)) {
                    "name" -> {
                        data.name = null
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile. Removed: **[]**".apply("Name"), this, event)
                    }
                    "from" -> {
                        data.from = null
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile. Removed: **[]**".apply("From"), this, event)
                    }
                    "language", "languages" -> {
                        data.languages = null
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile. Removed: **[]**".apply("Languages"), this, event)
                    }
                    "about" -> {
                        data.aboutMe = null
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile. Removed: **[]**".apply("About"), this, event)
                    }
                    "spotify" -> {
                        data.spotifyId = null
                        register.database.update(data)
                        register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + "Updated your profile. Removed: **[]**".apply("Spotify Account Connection"), this, event)
                    }
                    else -> onInvoke(event, listOf("help"), flags, register)
                }
            }
            else -> {
                event.message.addReaction(Emojis.THUMBS_UP.symbol).queue()

                val user = event.message.mentionedUsers.getOrNull(0) ?: event.author
                val data = register.database.getUserData(user)

                val embed = getEmbed("[]'s Profile".apply(user.display()), event.author, event.guild)
                        .setThumbnail(user.effectiveAvatarUrl)
                        .appendDescription("**IRL**" + "\n" + "------------\n")
                        .appendDescription("__Name__:" + " " + (data.name ?: "Not set!") + "\n")
                        .appendDescription("__From__:" + " " + (data.from ?: "Not set!") + "\n")
                        .appendDescription("__Speaks__:" + " " + (data.languages?.joinToString() ?: "Not set!") + "\n")
                        .appendDescription("__About me__: " + (data.aboutMe?.let { "```$it```" } ?: "Not set!") + "\n")
                        .appendDescription("\n")
                        .appendDescription("**Discord**" + "\n" + "------------\n")
                        .appendDescription("__Balance__: []".apply("$${data.money}") + "\n")
                        .appendDescription("__Married to__: []"
                                .apply(register.database.getMarriageFor(user.id)?.let { marriage ->
                                    (if (marriage.first == user.id) marriage.second else marriage.first)
                                            .toUser(register)?.display()
                                } ?: "No one!"))
                        .appendDescription("\n\n**Music**" + "\n" + "------------\n")
                        .appendDescription("__Spotify username__: []".apply((data.spotifyId ?: "Not set up!")))
                        .appendDescription("\n\n")
                        .appendDescription("\nDid you know? You can see your profile at []".apply("$base/profile/${user.id}") + "\n")
                        .appendDescription("**Want to edit your profile? Type _/profile help_ to learn how**")
                register.sender.cmdSend(embed, this, event, callback = { message ->
                    message.addReaction(Emojis.HEAVY_MULTIPLICATION_X.symbol).queue()
                    Sender.waitForReaction(
                            {
                                message.id == it.messageId && event.author.id == it.user.id && Emojis.HEAVY_MULTIPLICATION_X.symbol == it.reactionEmote.name
                            }, { message.delete().queue() }, expiration = { message.delete().queue() }, time = 60)
                })
            }
        }
    }

    val help = Argument("help")
}