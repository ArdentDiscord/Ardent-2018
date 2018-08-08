package com.ardentbot.core.database

import com.ardentbot.commands.admin.Announcement
import com.ardentbot.commands.games.*
import com.ardentbot.commands.music.DatabaseMusicLibrary
import com.ardentbot.commands.music.DatabaseMusicPlaylist
import com.ardentbot.commands.music.ServerQueue
import com.ardentbot.core.ArdentRegister
import com.google.gson.GsonBuilder
import com.rethinkdb.RethinkDB.r
import com.rethinkdb.net.Cursor
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.apache.commons.lang3.RandomStringUtils
import org.json.simple.JSONObject
import org.jsoup.Jsoup

private val conn = r.connection().hostname("rethinkdb").port(28015).db("ardent").connect()

class Database(val register: ArdentRegister) {
    val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()

    init {
        if (!r.dbList().run<List<String>>(conn).contains("ardent")) {
            println("Creating database 'ardent'")
            r.dbCreate("ardent").run<Any>(conn)
        }

        val tables = listOf(
                "BettingData",
                "BlackjackData",
                "Connect_4Data",
                "SlotsData",
                "Tic_Tac_ToeData",
                "TriviaData",
                "aggregated_status",
                "announcements",
                "commands",
                "guilds",
                "logs",
                "marriages",
                "music_libraries",
                "music_played",
                "music_playlists",
                "music_queues",
                "music_settings",
                "mutes",
                "staff",
                "status_changes",
                "users")
        tables.forEach { table ->
            if (!r.db("ardent").tableList().run<List<String>>(conn).contains(table)) {
                println("Creating table '$table'")
                r.db("ardent").tableCreate(table).run<Any>(conn)
            }
        }
    }

    private fun <T> deserializeSingleString(json: String, t: Class<T>): T = gson.fromJson(json, t)

    private fun <T> deserializeSingle(map: HashMap<*, *>?, t: Class<T>): T = deserializeSingleString(JSONObject.toJSONString(map), t)

    fun <T> deserializeWebsite(string: String, t: Class<T>): T {
        return deserializeSingleString(
                if (!string.startsWith("http")) string
                else Jsoup.connect(string).ignoreContentType(true).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.167 Safari/537.36").get().body().text()
                , t)
    }

    fun insert(obj: DbObject, blocking: Boolean = true) {
        val statement = r.table(obj.table).insert(r.json(gson.toJson(obj)))
        if (blocking) statement.run<Any>(conn) else statement.runNoReply(conn)
    }

    fun update(obj: DbObject, blocking: Boolean = true) {
        val statement = r.table(obj.table).get(obj.id).update(r.json(gson.toJson(obj)))
        if (blocking) statement.run<Any>(conn) else statement.runNoReply(conn)
    }

    fun delete(obj: DbObject, blocking: Boolean = true) {
        val statement = r.table(obj.table).get(obj.id).delete()
        if (blocking) statement.run<Any>(conn) else statement.runNoReply(conn)
    }

    fun get(table: String, id: Any): Any? = r.table(table).get(id).run(conn)
    fun delete(table: String, id: Any): Any? = r.table(table).get(id).delete().run(conn)
    fun delete(table: String) = r.table(table).delete().run<Any>(conn)

    fun getGuildData(guild: Guild): GuildData {
        var data = asPojo(r.table("guilds").get(guild.id).run(conn), GuildData::class.java)
        return if (data != null) data else {
            data = GuildData(guild.id, mutableListOf(), mutableListOf())
            insert(data)
            data
        }
    }

    fun getGuildMusicSettings(guild: Guild): MusicSettings {
        var data = asPojo(r.table("music_settings").get(guild.id).run(conn), MusicSettings::class.java)
        return if (data != null) data else {
            data = MusicSettings(guild.id)
            insert(data)
            data
        }
    }

    fun getMessagesFor(user: User, guild: Guild): List<UserMessage> {
        return r.table("logs").getAll(user.id).optArg("index", "userId").filter(r.hashMap("guildId", guild.id))
                .run<Any>(conn).queryAsArrayList(UserMessage::class.java).filterNotNull()
    }

    fun getMutes(): List<UserMute> {
        return r.table("mutes").run<Any>(conn).queryAsArrayList(UserMute::class.java).filterNotNull()
    }

    fun getTotalCommandsReceived(): Int = r.table("commands").count().run<Int>(conn)
    fun getTotalMessagesReceived(): Long = r.table("logs").count().run<Long>(conn)

    fun getStatusChanges(userId: String): List<StatusUpdate> {
        return r.table("status_changes").getAll(userId).optArg("index", "userId")
                .run<Any>(conn).queryAsArrayList(StatusUpdate::class.java).filterNotNull().sortedBy { it.time }
                .let { it.filterIndexed { i, _ -> i == it.lastIndex || it[i + 1].time - it[i].time > 1000 } }
    }

    fun getStatusChangeUsers() = r.table("status_changes").distinct().optArg("index", "userId")
            .run<Cursor<String>>(conn)

    fun getUserData(id: String): UserData {
        var data = asPojo(r.table("users").get(id).run(conn), UserData::class.java)
        if (data == null) {
            data = UserData(id, null, null, null, null)
            insert(data)
        }
        return data
    }

    fun getMusicLibrary(id: String): DatabaseMusicLibrary {
        var data = asPojo(r.table("music_libraries").get(id).run(conn), DatabaseMusicLibrary::class.java)
        if (data == null) {
            data = DatabaseMusicLibrary(id, mutableListOf())
            insert(data)
        }
        return data
    }

    fun getMarriageFor(id: String): Marriage? {
        return r.table("marriages").filter { it.g("first").eq(id).or(it.g("second").eq(id)) }.run<Any>(conn)
                .queryAsArrayList(Marriage::class.java).getOrNull(0)
    }

    fun getUsersSize() = r.table("users").count().run<Long>(conn)

    fun getRichestUsers(page: Int) = r.table("users").orderBy().optArg("index", r.desc("money")).slice(((page - 1) * 10))
            .limit(20).run<Any>(conn).queryAsArrayList(UserData::class.java).filterNotNull().take(10)

    fun getAnnouncements(): List<Announcement> {
        return r.table("announcements").run<Any>(conn).queryAsArrayList(Announcement::class.java).filterNotNull()
                .sortedBy { it.creationTime }
    }

    fun getAnnouncements(guild: Guild): List<Announcement> = getAnnouncements().filter { it.guildId == guild.id }

    fun getBlackjackGames(): List<GameDataBlackjack> = r.table("BlackjackData").run<Any>(conn).queryAsArrayList(GameDataBlackjack::class.java)
            .filterNotNull()

    fun getTriviaGames(): List<GameDataTrivia> = r.table("TriviaData").run<Any>(conn).queryAsArrayList(GameDataTrivia::class.java)
            .filterNotNull()

    fun getConnect4Games(): List<GameDataConnect4> = r.table("Connect_4Data").run<Any>(conn).queryAsArrayList(GameDataConnect4::class.java)
            .filterNotNull()

    fun getTicTacToeGames(): List<GameDataTicTacToe> = r.table("Tic_Tac_ToeData").run<Any>(conn).queryAsArrayList(GameDataTicTacToe::class.java)
            .filterNotNull()

    fun getSlotsGames(): List<GameDataSlots> = r.table("SlotsData").run<Any>(conn).queryAsArrayList(GameDataSlots::class.java)
            .filterNotNull()

    fun getBettingGames(): List<GameDataBetting> = r.table("BettingData").run<Any>(conn).queryAsArrayList(GameDataBetting::class.java)
            .filterNotNull()

    fun getStaff(): List<Staff> = r.table("staff").run<Any>(conn).queryAsArrayList(Staff::class.java).filterNotNull()

    fun getSavedQueues(): List<ServerQueue> = r.table("music_queues").run<Any>(conn).queryAsArrayList(ServerQueue::class.java)
            .filterNotNull()


    fun getPlaylists(id: String): List<DatabaseMusicPlaylist> {
        val playlists = mutableListOf<DatabaseMusicPlaylist>()
        r.table("music_playlists").filter { r.hashMap("owner", id) }.run<Any>(conn).queryAsArrayList(DatabaseMusicPlaylist::class.java)
                .forEach { if (it != null) playlists.add(it) }
        return playlists
    }
}

fun Database.getUserData(user: User): UserData = getUserData(user.id)
fun Guild.getUsersData(register: ArdentRegister) = members.map { register.database.getUserData(it.user) }

fun genId(length: Int, table: String?): String {
    val gen = RandomStringUtils.randomAlphanumeric(length)
    return if (table == null) gen
    else {
        if (r.table(table).get(gen).run<Any?>(conn) != null) genId(length, table)
        else gen
    }
}


abstract class DbObject(var id: Any = r.uuid().run(conn), val table: String)
