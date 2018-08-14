package com.ardentbot.commands.rpg

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import com.ardentbot.kotlin.Emojis
import com.ardentbot.kotlin.apply
import com.ardentbot.kotlin.localeDate
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit

@ModuleMapping("rpg")
class Daily : Command("daily", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        val data = register.database.getUserData(event.author.id)
        if (data.dailyCollectedAt.isEmpty() ||
                data.dailyCollectedAt.max()!! + TimeUnit.DAYS.toMillis(1) <= System.currentTimeMillis()) {
            val amount = register.random.nextInt(480) + 1 + 20
            data.money += amount
            data.dailyCollectedAt.add(System.currentTimeMillis())
            register.database.update(data)
            register.sender.cmdSend(Emojis.HEAVY_CHECK_MARK.cmd + translate("daily.collect", event, register)
                        .apply("**$amount**"), this, event)
        } else register.sender.cmdSend(Emojis.HEAVY_MULTIPLICATION_X.cmd + translate("daily.use_later",event, register)
                .apply("**" + (data.dailyCollectedAt.last() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)).localeDate() + "**"), this, event)
    }
}