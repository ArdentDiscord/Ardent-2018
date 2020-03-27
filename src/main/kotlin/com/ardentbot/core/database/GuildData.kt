package com.ardentbot.core.database

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.translation.Language
import net.dv8tion.jda.api.entities.Guild

class GuildData(id: String, val prefixes: MutableList<ArdentPrefix>, val disabledModules: MutableList<DisabledModule>,
                val disabledCommands: MutableList<DisabledCommand> = mutableListOf(), var adminRoleId: String? = null,
                var logChannel: String? = null, val eventsLogged: MutableList<String> = mutableListOf(),
                var muteRoleId: String? = null, val joinMessage: EventMessage = EventMessage(null, null),
                val leaveMessage: EventMessage = EventMessage(null, null), var defaultRoleId: String? = null,
                var language: Language? = null, var antiAdvertisingSettings: AntiAdvertisingSettings? = null,
                var antispamCooldownSeconds: Int? = null, var autoroles: MutableList<Autorole>? = null) : DbObject(id, "guilds") {
    fun prefixesModified(register: ArdentRegister): List<ArdentPrefix> {
        val withDefaults = if (register.config.test) mutableListOf(ArdentPrefix(".", register.selfUser.id, 0))
        else mutableListOf(ArdentPrefix("/", register.selfUser.id, 0), ArdentPrefix("ardent ", register.selfUser.id, 0),
                ArdentPrefix(register.selfUser.asMention, register.selfUser.id, 0))
        withDefaults.addAll(prefixes)
        return withDefaults
    }
}

class ArdentPrefix(val prefix: String, adder: String, addDate: Long) : AccountableData(adder, addDate)
class DisabledCommand(val name: String, adder: String, addDate: Long) : AccountableData(adder, addDate)
class DisabledModule(val name: String, adder: String, addDate: Long) : AccountableData(adder, addDate)
data class EventMessage(var channelId: String?, var message: String?)

abstract class AccountableData(val adder: String, val addDate: Long)

class GuildDataManager(val guild: Guild, register: ArdentRegister, val data: GuildData = register.database.getGuildData(guild))

class MusicSettings(id: String, var autoplay: Boolean = false, var stayInChannel: Boolean = false, var whitelistedRoles: MutableList<String>? = null,
                    var canEveryoneUseAdminCommands: Boolean = false, var whitelistedRolesForAdminCommands: MutableList<String>? = null)
    : DbObject(id, table = "music_settings")

data class AntiAdvertisingSettings(var allowServerLinks: Boolean, var banAfterTwoInfractions: Boolean)

/**
 * When [whitelistedRoles] is empty, all will be able to use this autorole
 */
data class Autorole(var name: String, var role: String, val creator: String, val whitelistedRoles: MutableList<String> = mutableListOf())

fun Guild.getLanguage(register: ArdentRegister) = register.database.getGuildData(this).language