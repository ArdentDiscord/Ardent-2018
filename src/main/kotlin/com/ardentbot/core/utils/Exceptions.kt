package com.ardentbot.core.utils

import net.dv8tion.jda.api.entities.Guild

class IllegalArgumentApplicationException(override val message: String) : Exception(message)
class UnknownModuleFoundException(override val message: String) : Exception(message)
class PrefixNotFoundException(override val message: String):Exception(message)
class DatabaseException(val guild: Guild):Exception("Unable to find guild ${guild.name} (${guild.id}) in the Ardent database")