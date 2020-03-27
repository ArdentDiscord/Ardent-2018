package com.ardentbot.web

import com.ardentbot.core.database.ArdentPrefix
import com.ardentbot.core.database.Autorole
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import spark.ModelAndView
import spark.Spark

fun Web.manage() {

    // MANAGEMENT
    Spark.path("/manage") {
        Spark.get("/:type/:action") { request, response ->
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
                        "autoroles" -> {
                            when (request.params(":action")) {
                                "add" -> {
                                    if (guild.getMember(user)?.hasPermission(Permission.MANAGE_SERVER) == true) {
                                        val autorole = request.queryParams("autorolename")
                                        val role = request.queryParams("autorolerole")
                                        if (autorole != null && role != null) {
                                            if (data.autoroles == null) data.autoroles = mutableListOf()
                                            data.autoroles!!.add(Autorole(autorole, role, user.id))
                                            register.database.update(data)
                                        }
                                    }
                                }
                                "remove" -> {
                                    if (guild.getMember(user)?.hasPermission(Permission.MANAGE_SERVER) == true) {
                                        val autorole = request.queryParams("autorolename")
                                        if (autorole != null) {
                                            data.autoroles?.removeIf { it.name == autorole }
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
}