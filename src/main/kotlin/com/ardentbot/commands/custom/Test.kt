/*package com.ardentbot.commands.custom

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

@ModuleMapping("info")
@MockCommand("test description")
@MockTranslations(MockTr("send", "hello world"),MockTr("send2", "hello world222"))
class Test : Command("test", null, null) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        event.channel.sendMessage(translate("test.send", event, register) + " " + translate("test.send2", event, register)).queue()
    }
}*/