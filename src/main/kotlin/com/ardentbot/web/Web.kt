package com.ardentbot.web

import com.adamratzman.spotify.utils.SimpleArtist
import com.ardentbot.commands.ardent.Internals
import com.ardentbot.commands.games.*
import com.ardentbot.commands.music.*
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.ExternalAction
import com.ardentbot.core.database.*
import com.ardentbot.core.toUser
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.display
import com.ardentbot.kotlin.localeDate
import com.ardentbot.kotlin.toMinutesAndSeconds
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Options
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import org.apache.commons.lang3.exception.ExceptionUtils
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine

val handlebars = HandlebarsTemplateEngine()

class Web(val register: ArdentRegister) {
    val redirects = hashMapOf<String, String>()

    init {
        base = if (register.config.test) "http://192.168.99.100:443" else "https://ardentbot.com"
        loginRedirect = "$base/api/oauth/login"

        registerHandlebarHelpers()
        startup()

        // HOMEPAGE

        get("/", { request, response ->
            val map = request.getDefaultMap(response, "Home")
            ModelAndView(map, "index.hbs")
        }, handlebars)

        // STATUS PAGE

        get("/status", { request, response ->
            val map = request.getDefaultMap(response, "Status")
            map["internals"] = Internals(register)
            map["arePeoplePlayingMusic"] = false
            ModelAndView(map, "status.hbs")
        }, handlebars)

        // COMMAND DESCRIPTIONS

        path("/commands") {
            get("") { request, response -> webRedirect(request, response, "/commands/") }
            get("/*") { request, response ->
                val map = request.getDefaultMap(response, "Commands")
                when {
                    request.splat().isEmpty() -> {
                        val commandWrappers = mutableListOf<CommandWrapper>()
                        register.holder.modules.forEach { (module, commands) ->
                            commandWrappers.add(CommandWrapper(module.name, module.id, commands.sortedBy { it.name }))
                        }
                        commandWrappers.sortBy { it.categoryName }
                        map["wrappers"] = commandWrappers
                        handlebars.render(ModelAndView(map, "commands.hbs"))
                    }
                    request.splat()[0] == "translate" -> handlebars.render(ModelAndView(map, "languages.hbs"))
                    else -> null
                }
            }
        }

        // GAMES PATH

        path("/games") {

            // RECENT GAMES
            get("/recent") { request, response ->
                val map = request.getDefaultMap(response, "Recent Games")
                val games = mutableListOf<Pair<GameType, GameData>>()
                register.database.getBettingGames().forEach { games.add(GameType.BETTING to it) }
                register.database.getSlotsGames().forEach { games.add(GameType.SLOTS to it) }
                register.database.getBlackjackGames().forEach { games.add(GameType.BLACKJACK to it) }
                register.database.getTriviaGames().forEach { games.add(GameType.TRIVIA to it) }
                register.database.getTicTacToeGames().forEach { games.add(GameType.TIC_TAC_TOE to it) }
                register.database.getConnect4Games().forEach { games.add(GameType.CONNECT_4 to it) }

                games.sortByDescending { it.second.endTime }
                map["total"] = games.size
                games.removeIf { it.second.creator.toUser(register) == null }
                map["recentGames"] = games.map {
                    SanitizedGame(it.second.creator.toUser(register)!!.display(), it.second.endTime.localeDate(), it.first.readable, "https://ardentbot.com/games/${it.first.readable.toLowerCase()}/${it.second.id}")
                }.take(30)
                handlebars.render(ModelAndView(map, "recentgames.hbs"))
            }

            // GAME RESULTS
            get("/*/*", { request, response ->
                val map = request.getDefaultMap(response, "")
                when (request.splat()[0]) {
                    "guess_the_number" -> {
                        map["title"] = "Guess The Number"
                        ModelAndView(map, "guessthenumber.hbs")
                    }
                    "blackjack" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("BlackjackData", id)?.let { asPojo(it as HashMap<*, *>, GameDataBlackjack::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            val user = game.creator.toUser(register)!!
                            map["title"] = "Blackjack Game #$id"
                            map["game"] = game
                            map["idLong"] = (game.id as Double).toLong()
                            map["user"] = user
                            map["date"] = game.startTime.localeDate()
                            map["data"] = user.getData(register)
                            ModelAndView(map, "blackjack.hbs")
                        }
                    }
                    "slots" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("SlotsData", id)?.let { asPojo(it as HashMap<*, *>, GameDataSlots::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            val user = game.creator.toUser(register)!!
                            map["title"] = "Slots Game #$id"
                            map["game"] = game
                            map["idLong"] = (game.id as Double).toLong()
                            map["user"] = user
                            map["date"] = game.startTime.localeDate()
                            map["data"] = user.getData(register)
                            ModelAndView(map, "slots.hbs")
                        }
                    }
                    "connect_4" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("Connect_4Data", id)?.let { asPojo(it as HashMap<*, *>, GameDataConnect4::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            val user = game.creator.toUser(register)!!
                            map["title"] = "Connect 4 Game #$id"
                            map["game"] = game
                            map["board"] = game.game.replace("\n", "<br />").replace("⚪", "◯")
                            map["winner"] = game.winner.toUser(register)!!
                            map["loser"] = game.loser.toUser(register)!!
                            map["user"] = user
                            map["idLong"] = (game.id as Double).toLong()
                            map["date"] = game.startTime.localeDate()
                            map["data"] = user.getData(register)
                            ModelAndView(map, "connect_4.hbs")
                        }
                    }
                    "tic_tac_toe" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("Tic_Tac_ToeData", id)?.let { asPojo(it as HashMap<*, *>, GameDataTicTacToe::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            map["title"] = "Tic Tac Toe Game #$id"
                            map["game"] = game
                            map["user"] = game.creator.toUser(register)!!
                            map["board"] = game.game.replace("\n", "<br />")
                            if (game.winner == null) {
                                map["hasWinner"] = false
                                map["player1"] = game.playerOne.toUser(register)!!
                                map["player2"] = game.playerTwo.toUser(register)!!
                            } else {
                                map["hasWinner"] = true
                                map["winner"] = game.winner.toUser(register)!!
                                map["loser"] = (if (game.winner != game.playerOne) game.playerOne else game.playerTwo).toUser(register)!!
                            }
                            map["idLong"] = (game.id as Double).toLong()
                            map["date"] = game.startTime.localeDate()
                            ModelAndView(map, "tic_tac_toe.hbs")
                        }
                    }
                    "trivia" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("TriviaData", id)?.let { asPojo(it as HashMap<*, *>, GameDataTrivia::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            val user = game.creator.toUser(register)!!
                            map["title"] = "Trivia Game #$id"
                            map["game"] = game.sanitize(register)
                            map["user"] = user
                            map["idLong"] = (game.id as Double).toLong()
                            map["date"] = game.startTime.localeDate()
                            map["data"] = user.getData(register)
                            ModelAndView(map, "trivia.hbs")
                        }
                    }
                    "betting" -> {
                        val id = request.splat()[1].toDoubleOrNull()?.toInt() ?: 999999999
                        val game = register.database.get("BettingData", id)?.let { asPojo(it as HashMap<*, *>, GameDataBetting::class.java) }
                        if (game == null) {
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "No game with that id was found!"
                            map["title"] = "Gamemode not found"
                            ModelAndView(map, "404.hbs")
                        } else {
                            val creator = game.creator.toUser(register)!!
                            map["title"] = "Betting Game #$id"
                            map["idLong"] = (game.id as Double).toLong()
                            map["game"] = game
                            map["user"] = creator
                            map["date"] = game.startTime.localeDate()
                            ModelAndView(map, "betting.hbs")
                        }
                    }
                    else -> {
                        map["showSnackbar"] = true
                        map["snackbarMessage"] = "No Gamemode with that title was found!"
                        map["title"] = "Gamemode not found"
                        ModelAndView(map, "404.hbs")
                    }
                }
            }, handlebars)
        }

        // EXTERNAL ACTIONS

        path("/dynamic") {
            // SELECTING A DATETIME
            get("/datetime/:random/:random1") { request, response ->
                if (ExternalAction.currentlyUsedExternalActions.map { it.first }.contains(request.pathInfo())) {
                    val map = request.getDefaultMap(response, "Action In Use")
                    handlebars.render(ModelAndView(map, "action_in_use.hbs"))
                } else if (!ExternalAction.waitingCallbacks.map { it.first }.contains(request.pathInfo())) {
                    val map = request.getDefaultMap(response, "404 - Not Found")
                    handlebars.render(ModelAndView(map, "404.hbs"))
                } else {
                    val map = request.getDefaultMap(response, "Date + Time Chooser")
                    map["random"] = request.params(":random")
                    map["random1"] = request.params(":random1")
                    map["request-identifier"] = register.random.nextLong()
                    ExternalAction.currentlyUsedExternalActions.add(request.pathInfo() to map["request-identifier"]!! as Long)
                    handlebars.render(ModelAndView(map, "time-selector.hbs"))
                }
            }

            // SUCCESS POST-ACTION PAGE
            get("/success") { request, response ->
                val map = request.getDefaultMap(response, "Successful action")
                handlebars.render(ModelAndView(map, "action_successful.hbs"))
            }

            // ACTION PROCESSING
            path("/accept") {
                get("/time/:random/:random1") { request, response ->
                    val requestIdentifier = request.queryParams("request-identifier")?.toLongOrNull()
                    val editedUrl = "/dynamic/datetime/${request.params(":random")}/${request.params(":random1")}"
                    if (ExternalAction.currentlyUsedExternalActions.firstOrNull { requestIdentifier == it.second }?.first
                            != editedUrl) {
                        handlebars.render(ModelAndView(request.getDefaultMap(response, "Bad Request"), "bad_request.hbs"))
                    } else {
                        val waitingCallback = ExternalAction.waitingCallbacks.first { it.first == editedUrl }
                        waitingCallback.second(request.queryParams("time").toLong())
                        ExternalAction.waitingCallbacks.remove(waitingCallback)
                        ExternalAction.currentlyUsedExternalActions.removeIf { it.first == request.pathInfo() }
                        response.redirect("/dynamic/success")
                    }
                }
            }
        }


        // API METHODS
        path("/api") {
            // OAUTH
            path("/oauth") {
                get("/spotify") { request, response ->
                    if (request.queryParams("code") == null || request.queryParams("state") == null) {
                        response.redirect("/welcome")
                        null
                    } else {
                        val code = request.queryParams("code")
                        val state = request.queryParams("state")
                        try {
                            val client = register.spotifyApi.authorizeUser(code, "$base/api/oauth/spotify", false)
                            val waitingCallback = ExternalAction.waitingCallbacks.firstOrNull { it.first == state }
                            if (waitingCallback == null) {
                                response.redirect("/fail")
                            } else {
                                ExternalAction.waitingCallbacks.remove(waitingCallback)
                                waitingCallback.second.invoke(client)
                                response.redirect("/dynamic/success")
                            }
                        } catch (e: Exception) {
                            response.redirect("/welcome")
                        }
                    }
                }
                get("/login", { request, response ->
                    if (request.queryParams("code") == null) {
                        response.redirect("/welcome")
                        null
                    } else {
                        val code = request.queryParams("code")
                        val token = retrieveToken(code, register)
                        if (token == null) {
                            response.redirect("/welcome")
                            null
                        } else {
                            val identification = identityObject(token.access_token)
                            if (identification != null) {
                                val session = request.session()
                                val role = register.database.get("staff", identification.id)?.let { asPojo(it as HashMap<*, *>, Staff::class.java) }
                                if (role != null) session.attribute("role", role)
                                session.attribute("user", register.getUser(identification.id))
                            }
                            response.redirect(redirects[request.session().id()] ?: "/getting-started")
                            null
                        }
                    }
                }, handlebars)
                post("/login") { _, response -> response.redirect("/getting-started") }
            }

            // MUSIC
            path("/music") {
                get("/:action") { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    when (user) {
                        null -> {
                            response.redirect("/login")
                        }
                        else -> {
                            when (request.params(":action")) {
                                "addsong", "add-song" -> {
                                    val url = request.queryParams("song")
                                    val playlistId = request.queryParams("playlistId")
                                    if (playlistId == null) {
                                        val library = register.database.getMusicLibrary(user.id)
                                        url.loadExternally(register) { audioTrack, _ ->
                                            library.lastModified = System.currentTimeMillis()
                                            library.tracks.add(DatabaseTrackObj(user.id, System.currentTimeMillis(), null, audioTrack.info.title,
                                                    audioTrack.info.author, if (url.startsWith("https")) url else audioTrack.info.uri))
                                            register.database.update(library)
                                        }
                                        response.redirect("/profile?add=true")
                                    } else {
                                        val playlist = getPlaylistById(playlistId, register)
                                        if (playlist?.owner?.equals(user.id) == true) {
                                            url.loadExternally(register) { audioTrack, _ ->
                                                playlist.lastModified = System.currentTimeMillis()
                                                playlist.tracks.add(DatabaseTrackObj(user.id, System.currentTimeMillis(), playlist.id as String, audioTrack.info.title,
                                                        audioTrack.info.author, if (url.startsWith("https")) url else audioTrack.info.uri))
                                                register.database.update(playlist)
                                            }
                                            response.redirect("/music/playlist/${playlist.id}?add=true")
                                        }
                                    }
                                }
                                "removesong" -> {
                                    val index = request.queryParams("song")?.toIntOrNull()
                                    val library = register.database.getMusicLibrary(user.id)
                                    if (index in 0..(library.tracks.size - 1)) library.tracks.removeAt(index!!)
                                    library.lastModified = System.currentTimeMillis()
                                    register.database.update(library)
                                    response.redirect(request.queryParams("request") ?: "/profile")
                                }
                                "removefromplaylist" -> {
                                    val index = request.queryParams("song")?.toIntOrNull()
                                    val playlist = getPlaylistById(request.queryParams("playlist") ?: "", register)
                                    if (index != null && playlist?.tracks != null && index in 0..(playlist.tracks.size - 1) && playlist.owner == user.id) {
                                        playlist.tracks.removeAt(index)
                                        register.database.update(playlist)
                                        response.redirect("/music/playlist/${playlist.id}")
                                    } else response.redirect(request.queryParams("request") ?: "/profile")
                                }
                                "cloneplaylist" -> {
                                    val playlistId = request.queryParams("playlistId")
                                    if (playlistId != null) {
                                        val playlist = getPlaylistById(playlistId, register)
                                        if (playlist != null && playlist.owner != user.id) {
                                            val id = genId(6, "music_playlists")
                                            register.database.insert(
                                                    DatabaseMusicPlaylist(id, user.id, playlist.name, playlist.lastModified, playlist.spotifyAlbumId,
                                                            playlist.spotifyPlaylistId, playlist.youtubePlaylistUrl, playlist.tracks)
                                            )
                                            response.redirect("/music/playlist/$id")
                                        }
                                    }
                                    response.redirect("/404")
                                }
                            }
                        }
                    }
                }
            }

            // MANAGEMENT
            path("/manage") {
                get("/:type/:action") { request, response ->
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    val guild = register.getGuild(request.queryParams("guild"))
                    when {
                        user == null -> {
                            redirects[request.session().id()] = "/manage/${guild?.id}"
                            response.redirect("/login")
                            null
                        }
                        guild == null -> {
                            response.redirect("/manage")
                            null
                        }
                        else -> {
                            val map = request.getDefaultMap(response, "Data Response")
                            val data = register.database.getGuildData(guild)
                            when (request.params(":type")) {
                                "prefix" -> {
                                    when (request.params(":action")) {
                                        "add" -> {
                                            request.queryParams("prefix")?.let { prefix ->
                                                if (prefix.isNotBlank() && data.prefixes.find { it.prefix == prefix } == null) {
                                                    data.prefixes.add(ArdentPrefix(prefix, user.id, System.currentTimeMillis()))
                                                    register.database.update(data)
                                                }
                                            }
                                        }
                                        "remove" -> {
                                            request.queryParams("prefix")?.let { prefix ->
                                                if (prefix != "/" && prefix != "ardent") {
                                                    data.prefixes.removeIf { it.prefix == prefix }
                                                    register.database.update(data)
                                                }
                                            }
                                        }
                                        else -> {
                                            map["title"] = "404 Not Found"
                                            handlebars.render(ModelAndView(map, "404.hbs"))
                                        }
                                    }
                                }
                                "roles" -> {
                                    when (request.params(":action")) {
                                        "default" -> {
                                            request.queryParams("defaultRole")?.let { defaultRoleId ->
                                                if (defaultRoleId.isNotBlank()) {
                                                    if (defaultRoleId == "none") data.defaultRoleId = null
                                                    else data.defaultRoleId = defaultRoleId
                                                    register.database.update(data)
                                                }
                                            }
                                        }
                                        else -> {
                                            map["title"] = "404 Not Found"
                                            handlebars.render(ModelAndView(map, "404.hbs"))
                                        }
                                    }
                                }
                                "music" -> {
                                    val musicSettings = register.database.getGuildMusicSettings(guild)
                                    when (request.params(":action")) {
                                        "changeautoplay" -> {
                                            request.queryParams("state")?.let { state ->
                                                if (state == "on") {
                                                    musicSettings.autoplay = true
                                                } else if (state == "off") {
                                                    musicSettings.autoplay = false
                                                }
                                                register.database.update(musicSettings)
                                            }
                                        }
                                        "stayinvoice" -> {
                                            request.queryParams("state")?.let { state ->
                                                if (state == "on") {
                                                    musicSettings.stayInChannel = true
                                                } else if (state == "off") {
                                                    musicSettings.stayInChannel = false
                                                }
                                                register.database.update(musicSettings)
                                            }
                                        }
                                        "changemusicadmin" -> {
                                            request.queryParams("state")?.let { state ->
                                                if (state == "on") {
                                                    musicSettings.canEveryoneUseAdminCommands = true
                                                } else if (state == "off") {
                                                    musicSettings.canEveryoneUseAdminCommands = false
                                                }
                                                register.database.update(musicSettings)
                                            }
                                        }
                                        else -> {
                                            map["title"] = "404 Not Found"
                                            handlebars.render(ModelAndView(map, "404.hbs"))
                                        }
                                    }
                                }
                                else -> {
                                    map["title"] = "404 Not Found"
                                    handlebars.render(ModelAndView(map, "404.hbs"))
                                }
                            }
                            response.redirect("/manage/${guild.id}")
                        }
                    }
                }
            }

            // ADMINISTRATION
            path("/administrators") {
                get("/:type/:action") { request, response ->
                    val map = request.getDefaultMap(response, "Data Response")
                    val session = request.session()
                    val user: User? = session.attribute<User>("user")
                    val role: Staff? = session.attribute<Staff>("role")
                    when {
                        user == null -> {
                            redirects[request.session().id()] = "/administrators"
                            response.redirect("/login")
                            null
                        }
                        role?.role != Staff.StaffRole.ADMINISTRATOR -> {
                            map["title"] = "No permission"
                            map["showSnackbar"] = true
                            map["snackbarMessage"] = "You need to be an administrator to access this page!"
                            handlebars.render(ModelAndView(map, "fail.hbs"))
                        }
                        else -> when (request.params(":type")) {
                            "staff" -> {
                                when (request.params(":action")) {
                                    "add" -> {
                                        request.queryParams("id")?.let { id ->
                                            if (register.getUser(id) != null) {
                                                register.database.insert(Staff(id, Staff.StaffRole.MODERATOR))
                                            }
                                        }
                                        response.redirect("/administrators")
                                    }
                                    "remove" -> {
                                        val id = request.queryParams("id") ?: "invalid"
                                        register.database.delete("staff", id)
                                        response.redirect("/administrators")
                                    }
                                    else -> {
                                        map["title"] = "404 Not Found"
                                        handlebars.render(ModelAndView(map, "404.hbs"))
                                    }
                                }
                            }
                            else -> {
                                map["title"] = "404 Not Found"
                                handlebars.render(ModelAndView(map, "404.hbs"))
                            }
                        }
                    }
                }
            }

        }

        // Getting Started
        get("/getting-started", { request, response ->
            val map = request.getDefaultMap(response, "Getting Started")
            map["showSnackbar"] = false
            map["internals"] = Internals(register)
            ModelAndView(map, "getting_started.hbs")
        }, handlebars)

        // Management
        get("/manage", { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            if (user == null) {
                redirects[session.id()] = "/manage"
                response.redirect("/login")
                null
            } else {
                val map = request.getDefaultMap(response, "Ardent Management Center")
                map["showSnackbar"] = false
                map["guilds"] = register.getMutualGuildsWith(user).mapNotNull { it.getMember(user) }.filter { member ->
                    member.hasPermission(Permission.MANAGE_SERVER) ||
                            register.database.getGuildData(member.guild).let { data ->
                                data.adminRoleId?.let { member.guild.getRoleById(it)?.let { member.roles.contains(it) } } == true
                            }
                }.map { it.guild }.sortedBy { it.name }
                ModelAndView(map, "manage.hbs")
            }
        }, handlebars)

        get("/manage/*", { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            val guild = register.getGuild(request.splat().getOrNull(0) ?: "4")
            if (user == null) {
                redirects[request.session().id()] = "/manage/${guild?.id}"
                response.redirect("/login")
                null
            } else if (guild == null) {
                response.redirect("/manage")
                null
            } else {
                val map = request.getDefaultMap(response, "Management | []".apply(guild.name))
                map["showSnackbar"] = false
                val member = guild.getMember(user)

                if (member.hasPermission(Permission.MANAGE_SERVER) ||
                        register.database.getGuildData(member.guild).let { data ->
                            data.adminRoleId?.let { member.guild.getRoleById(it)?.let { member.roles.contains(it) } } == true
                        }) {
                    val data = register.database.getGuildData(guild)
                    map["guild"] = guild
                    map["prefixesEmpty"] = data.prefixes.isEmpty()
                    map["musicSettings"] = register.database.getGuildMusicSettings(guild)
                    map["defaultRole"] = data.defaultRoleId?.let { guild.getRoleById(it) }
                    map["guildData"] = data
                    ModelAndView(map, "manageGuild.hbs")
                } else {
                    map["showSnackbar"] = true
                    map["snackbarMessage"] = "You don't have permission to manage the settings for this server!"
                    ModelAndView(map, "fail.hbs")
                }
            }
        }, handlebars)

        get("/administrators", { request, response ->
            val map = request.getDefaultMap(response, "Administrator Zone")
            map["commands"] = register.holder.commands.sortedBy { it.name }
            map["staffMembers"] = register.database.getStaff().filterNot { it.role == Staff.StaffRole.ADMINISTRATOR }.map { (it.id as String).toUser(register) }
            if (isAdministrator(request, response)) {
                map["showSnackbar"] = false
                ModelAndView(map, "administrators.hbs")
            } else null
        }, handlebars)

        get("/logout") { request, response ->
            request.session().invalidate()
            response.redirect("/")
        }

        get("/fail", { request, response ->
            val map = request.getDefaultMap(response, "No Permission")
            ModelAndView(map, "fail.hbs")
        }, handlebars)

        get("/welcome", { request, response ->
            val map = request.getDefaultMap(response, "Welcome to Ardent!")
            ModelAndView(map, "welcome.hbs")
        }, handlebars)

        get("/profile") { request, response ->
            val session = request.session()
            val user: User? = session.attribute<User>("user")
            if (user == null) {
                redirects[session.id()] = "/profile"
                response.redirect("/login")
                null
            } else {
                val map = request.getDefaultMap(response, "Your user profile")
                val data = register.database.getUserData(user)
                val musicLibrary = register.database.getMusicLibrary(user.id)
                val playlists = register.database.getPlaylists(user.id)
                map["profUser"] = user
                map["aboutMe"] = data.aboutMe?.replace("\n", "<br />")
                map["from"] = data.from
                map["languages"] = data.languages?.joinToString()
                map["money"] = data.money
                map["spotifyId"] = data.spotifyId
                map["hasLocalMusic"] = musicLibrary.tracks.isNotEmpty()
                map["musicLibrary"] = musicLibrary
                map["hasPlaylists"] = playlists.isNotEmpty()
                map["playlists"] = playlists
                map["isUser"] = true
                if (request.queryParams("add") == "true") {
                    map["snackbar"] = "Adding your track.. to see an updated list, click here in a few seconds"
                    map["snackbarUrl"] = "/profile"
                }
                handlebars.render(ModelAndView(map, "profile.hbs"))
            }
        }

        get("/profile/:id") { request, response ->
            val session = request.session()
            val meUser: User? = session.attribute<User>("user")
            val user: User? = request.params(":id")?.let { register.getUser(it) } ?: meUser
            if (user == null) {
                redirects[session.id()] = "/profile/${request.params(":id")}"
                response.redirect("/login")
                null
            } else {
                val map = request.getDefaultMap(response, "User Profile | []".apply(user.display()))
                val data = register.database.getUserData(user)
                val musicLibrary = register.database.getMusicLibrary(user.id)
                val playlists = register.database.getPlaylists(user.id)
                map["profUser"] = user
                map["aboutMe"] = data.aboutMe?.replace("\n", "<br />")
                map["from"] = data.from
                map["languages"] = data.languages?.joinToString()
                map["money"] = data.money
                map["spotifyId"] = data.spotifyId
                map["hasLocalMusic"] = musicLibrary.tracks.isNotEmpty()
                map["musicLibrary"] = musicLibrary
                map["hasPlaylists"] = playlists.isNotEmpty()
                map["musicPlaylists"] = playlists
                map["isUser"] = meUser?.id == user.id
                if (request.queryParams("add") == "true") {
                    map["snackbar"] = "Adding your track.. to see an updated list, click here in a few seconds"
                    map["snackbarUrl"] = "/profile"
                }
                handlebars.render(ModelAndView(map, "profile.hbs"))
            }
        }

        // Music
        path("/music") {
            // Server queues
            get("/queue/*") { request, response ->
                val map = request.getDefaultMap(response, "Queue")
                if (request.splat().isEmpty()) webRedirect(request, response, "/")
                else {
                    val guild = register.getGuild(request.splat()[0])
                    if (guild == null) webRedirect(request, response, "/")
                    else {
                        val manager = guild.getAudioManager(null, register).manager
                        if (manager.current?.track != null) {
                            map["isPlaying"] = true
                            map["current"] = TrackDisplay(manager.current!!.track!!.info.title, manager.current!!.track!!.info.author)
                        }
                        map["title"] = "Queue for ${guild.name}"
                        map["queue"] = manager.queue.toList().toTrackDisplay()
                        map["hasQueue"] = manager.queue.size > 0
                        map["guild"] = guild
                        map["info"] = "<b>${manager.queue.map {
                            it.track?.duration ?: 0
                        }.sum().toMinutesAndSeconds()}</b> | <b>${manager.queue.size}</b> tracks"
                        handlebars.render(ModelAndView(map, "queue.hbs"))
                    }
                }
            }
            // Playlists
            path("/playlist") {
                get("/*") { request, response ->
                    val map = request.getDefaultMap(response, "Playlists")
                    if (request.splat().isEmpty()) webRedirect(request, response, "/")
                    else {
                        val playlist = getPlaylistById(request.splat()[0], register)
                        if (playlist == null) webRedirect(request, response, "/")
                        else {
                            if (request.queryParams("add") == "true") {
                                map["snackbar"] = "Adding your track.. to see an updated list, click here in a few seconds"
                                map["snackbarUrl"] = "/music/playlist/${playlist.id}"
                            }
                            map["isUser"] = playlist.owner == (map["user"] as User?)?.id
                            map["playlist"] = playlist
                            map["ownerUser"] = register.getUser(playlist.owner)
                            when {
                                playlist.spotifyAlbumId != null -> {
                                    val album = register.spotifyApi.albums.getAlbum(playlist.spotifyAlbumId!!).complete()
                                    if (album != null) {
                                        map["albumTitle"] = album.name
                                        map["albumTracks"] = album.tracks.items
                                        map["albumArtists"] = album.artists.joinToString { it.name }
                                        map["albumLink"] = "https://open.spotify.com/album/${album.id}"
                                        map["albumInfo"] = "<b>${album.tracks.items.map { it.duration_ms }.sum().toLong().toMinutesAndSeconds()}</b> | <b>${album.tracks.total}</b> tracks"
                                    }
                                }
                                playlist.spotifyPlaylistId != null -> {
                                    val split = playlist.spotifyPlaylistId.split("||")
                                    val foundPlaylist = register.spotifyApi.playlists.getPlaylist(split[0], split[1]).complete()
                                    if (foundPlaylist != null) {
                                        map["playlistLink"] = "https://open.spotify.com/user/${foundPlaylist.owner.id}/playlist/${foundPlaylist.id}"
                                        map["playlistTitle"] = foundPlaylist.name
                                        map["playlistOwner"] = foundPlaylist.owner
                                        map["playlistDescription"] = foundPlaylist.description
                                        map["playlistTracks"] = foundPlaylist.tracks.items.map { it.track }
                                        map["playlistInfo"] = "<b>${foundPlaylist.tracks.items.map { it.track.duration_ms }.sum().toLong().toMinutesAndSeconds()}</b> | <b>${foundPlaylist.tracks.total}</b> tracks"
                                    }
                                }
                            }
                            map["dbTracks"] = playlist.tracks
                            map["trackInfo"] = "<b>${playlist.tracks.size}</b> tracks"
                            map["hasDbTracks"] = playlist.tracks.isNotEmpty()
                            handlebars.render(ModelAndView(map, "playlist.hbs"))
                        }
                    }
                }

            }
        }


        // Links

        get("/robots.txt") { _, _ ->
            "User-Agent: *\n" +
                    "Disallow: \n" +
                    "Disallow: /manage\n" +
                    "Disallow: /administrators\n" +
                    "Disallow: /dynamic"
        }

        get("/invite") { _, response ->
            response.redirect("https://discordapp.com/api/oauth2/authorize?client_id=339101087569281045&permissions=8&scope=bot")
        }
        get("/support") { _, response ->
            response.redirect("https://discord.gg/VebBB5z")
        }
        get("/hub") { _, response ->
            response.redirect("https://discord.gg/VebBB5z")
        }
        get("/patreon") { _, response ->
            response.redirect("https://www.patreon.com/ardent")
        }
        get("/github") { _, response ->
            response.redirect("https://github.com/ArdentDiscord/Ardent-2018")
        }
        get("/login") { _, response ->
            response.redirect("https://discordapp.com/oauth2/authorize?scope=identify&client_id=${register.selfUser.id}&response_type=code&redirect_uri=$loginRedirect")
        }
    }

    private fun startup() {
        if (!register.config.test) {
            secure(register.config.args[1], "ardent", null, null)
        }
        port(443)
        staticFiles.location("/public")
        exception(Exception::class.java) { exception, request, _ ->
            exception.printStackTrace()
            register.getTextChannel(register.config["error_channel"])!!.sendMessage(
                    "**Time:** ${System.currentTimeMillis()}\n" +
                            "**Path:** ${request.pathInfo()}\n" +
                            "**Params:** ${request.params()}\n" +
                            "**Message:** ${exception.localizedMessage}"
            ).queue()
            register.getTextChannel(register.config["error_channel"])!!.sendMessage("^\n" +
                    ExceptionUtils.getStackTrace(exception)).queue()
        }
        notFound { request, response ->
            val map = request.getDefaultMap(response, "404 - Not Found")
            handlebars.render(ModelAndView(map, "404.hbs"))
        }
    }

    private fun webRedirect(request: Request, response: Response, redirect_to: String, redirect_back: String? = null) {
        if (redirect_back != null) redirects[request.session().id()] = redirect_back
        response.redirect(redirect_to)
    }
}


fun Request.getDefaultMap(response: Response, title: String): HashMap<Any, Any?> {
    val map = hashMapOf<Any, Any?>()
    val session = session()
    map["title"] = title
    val user = session.attribute<User>("user")
    if (user == null) {
        map["validSession"] = false
    } else {
        map["validSession"] = true
        map["user"] = user
        val role = session.attribute<Staff>("role")
        if (role == null) {
            map["isStaff"] = false
            map["isAdmin"] = false
            map["hasWhitelists"] = false
        } else {
            map["isAdmin"] = role.role == Staff.StaffRole.ADMINISTRATOR
            map["isStaff"] = true
            map["role"] = role.role
        }
    }

    if (cookie("error") != null) {
        map["hasError"] = true
        map["error"] = cookie("error")
        response.removeCookie("error")
    }

    return map
}

fun registerHandlebarHelpers() {
    val field = handlebars::class.java.getDeclaredField("handlebars")
    field.isAccessible = true
    val handle = field.get(handlebars) as Handlebars
    handle.registerHelper("dateFancy") { date: Long, _: Options -> date.localeDate() }
    handle.registerHelper("artistConcat") { artist: List<SimpleArtist>, _: Options -> artist.joinToString { it.name } }
}


fun isAdministrator(request: Request, response: Response): Boolean {
    val session = request.session()
    val user: User? = session.attribute<User>("user")
    val role: Staff? = session.attribute<Staff>("role")
    if (user == null) {
        response.redirect("/login")
        return false
    } else if (role == null || role.role != Staff.StaffRole.ADMINISTRATOR) {
        response.redirect("/fail")
        return false
    }
    return true
}
