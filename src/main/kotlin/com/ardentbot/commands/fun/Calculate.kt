package com.ardentbot.commands.`fun`

import com.adamratzman.math.MathParser
import com.adamratzman.math.expressions.Expression
import com.ardentbot.commands.games.send
import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.concat
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@ModuleMapping("fun")
class Calculate : Command("calculate", arrayOf("calc"), null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val mathParser = MathParser(75)
        if (arguments.isEmpty()) event.channel.send(translate("calculate.help", event, register), register)
        else {
            arguments.concat().split(";").let {
                val query = it[0]
                val expression = Expression(query, mathParser)
                for (i in 1..it.lastIndex) {
                    val variableEvaluateString = it[i].trim()
                    val split = variableEvaluateString.split(" ")
                    if (split.size < 3 || split[1] != "=") {
                        event.channel.send(translate("calculate.invalid_variable_declaration", event, register), register)
                        return
                    } else {
                        println(split[0] + split.subList(2, split.size))
                        expression.set(split[0], split.subList(2, split.size).concat())
                    }
                }
                try {
                    val future = register.cachedExecutor.submit {
                        val result = expression.evaluate()
                        val value = result.result.stripTrailingZeros()
                        event.channel.send(translate("calculate.send", event, register).apply(value), register)
                    }
                    future.get(5, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                    event.channel.send(translate("calculate.timeout", event, register).apply(5), register)
                } catch (e: Throwable) {
                    event.channel.send(translate("calculate.error", event, register).apply(e.message
                            ?: e.localizedMessage), register)
                    e.printStackTrace()
                }
            }
        }
    }
}