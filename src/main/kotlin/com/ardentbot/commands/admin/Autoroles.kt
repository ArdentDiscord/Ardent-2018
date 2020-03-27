package com.ardentbot.commands.admin

import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.MockCommand
import com.ardentbot.core.commands.MockTr
import com.ardentbot.core.commands.MockTranslations
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis.SMALL_ORANGE_DIAMOND
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import com.ardentbot.kotlin.forEach
import com.ardentbot.kotlin.getEmbed
import com.ardentbot.kotlin.toRole
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("admin")
@MockCommand("gives you the role you wish to receive")
class Iam : Command("iam", arrayOf("giverole"), 5) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)

        if (arguments.isEmpty()) {
            val embed = getEmbed("Iam List", event.author, event.guild)
            val builder = StringBuilder().append("This is the **autoroles** list for *[]* - to add or delete them, type **/manage**".apply(event.guild.name) + "\n")
            if (data.autoroles?.isEmpty() != false) builder.append("This server doesn't have any autoroles :(")
            else {
                val iterator = data.autoroles!!.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    val role = it.role.toRole(event.guild)
                    if (role != null) builder.append("[] Typing /iam **[]** will give you the **[]** role".apply(SMALL_ORANGE_DIAMOND.symbol, it.name, role.name)).append("\n")
                    else iterator.remove()
                }
                register.database.update(data)

                builder.append("\n").append("**Give yourself one of these roles by typing _/iam NAME_**")
                builder.append("\n\n").append("**Tip**: You can create autoroles using */manage*")
            }
            var curr = 0
            while (curr < builder.length) {
                event.channel.send(embed.setDescription(builder.substring(curr, if ((curr + 2048) <= builder.length) curr + 2048 else builder.length)), register)
                curr += 2048
            }
        }else {
            var found = false
            if (data.autoroles == null) {
                data.autoroles= mutableListOf()
                register.database.update(data)
            }

            data.autoroles!!.forEach { iterator, current ->
                if (current.name.replace(" ", "").equals(arguments.concat().replace(" ", ""), true)) {
                    val role = current.role.toRole(event.guild)
                    if (role == null) {
                        iterator.remove()
                        register.database.update(data)
                    } else {
                        try {
                            event.guild.addRoleToMember(event.member!!, role).reason("Ardent Autoroles").queue({
                                event.channel.send("Successfully gave you the **[]** role!".apply(role.name),register)
                            }, {
                                event.channel.send("Failed to give the *[]* role - **Please ask an administrator of this server to allow me to give you roles!**".apply(role.name),register)
                            })
                        } catch (e: Throwable) {
                            event.channel.send("Failed to give the *[]* role - **Please ask an administrator of this server to allow me to give you roles!**".apply(role.name),register)
                        }
                    }
                    found = true
                    return@forEach
                }
            }
            if (!found) event.channel.send("An autorole with that name wasn't found. Please type */iam* to get a full list",register)
        }
    }
}

@ModuleMapping("admin")
@MockCommand("removes the role that you've been given via /iam")
class Iamnot : Command("iamnot", arrayOf("removerole"), 5) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getGuildData(event.guild)
        if (arguments.isEmpty()) {
            val embed = getEmbed("Iam List", event.author, event.guild)
            val builder = StringBuilder().append("This is the **autoroles** list for *[]* - to add or delete them, type **/manage**".apply(event.guild.name) + "\n")
            if (data.autoroles?.isEmpty() != false) builder.append("This server doesn't have any autoroles :(")
            else {
                val iterator = data.autoroles!!.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    val role = it.role.toRole(event.guild)
                    if (role != null) builder.append("[] Typing /iamnot **[]** will remove the **[]** role".apply(SMALL_ORANGE_DIAMOND.symbol, it.name, role.name)).append("\n")
                    else iterator.remove()
                }
                register.database.update(data)

                builder.append("\n").append("**Remove one of these roles by typing _/iamnot NAME_**")
                builder.append("\n\n").append("**Tip**: You can create or delete autoroles using */manage*")
            }
            var curr = 0
            while (curr < builder.length) {
                event.channel.send(embed.setDescription(builder.substring(curr, if ((curr + 2048) <= builder.length) curr + 2048 else builder.length)), register)
                curr += 2048
            }
            return
        }

        val name = arguments.concat()
        if (data.autoroles == null) {
            data.autoroles = mutableListOf()
            register.database.update(data)
        }

        val iterator = data.autoroles!!.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.name.equals(name, true)) {
                val role = it.role.toRole(event.guild)
                if (role == null) {
                    iterator.remove()
                    register.database.update(data)
                } else {
                    if (event.member!!.roles.contains(role)) {
                        try {
                            event.guild.removeRoleFromMember(event.member!!, role).reason("Ardent Autoroles - Removal").queue({
                                event.channel.send("Successfully removed the **[]** role!".apply(role.name),register)
                            }, {
                                event.channel.send("Failed to remove *[]* - **please ask an administrator of this server to allow me to manage roles!**".apply(role.name),register)
                            })
                        } catch (e: Exception) {
                            event.channel.send("Failed to remove *[]* - **please ask an administrator of this server to allow me to manage roles!**".apply(role.name),register)
                        }
                    } else event.channel.send("You can't remove a role you don't have!",register)
                }
                return
            }
        }
        event.channel.send("An autorole with that name wasn't found. Please type **/iam** to get a full list",register)
    }
}