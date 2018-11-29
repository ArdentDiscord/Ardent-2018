package com.ardentbot.commands.`fun`

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.commands.Command
import com.ardentbot.core.commands.ModuleMapping
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.jsoup.Jsoup
import java.lang.Exception
import java.net.SocketTimeoutException

@ModuleMapping("fun")
class FML : Command("fml", arrayOf("fuckmylife"), 5) {
    override fun onInvoke(event: GuildMessageReceivedEvent, arguments: List<String>, flags: List<Flag>, register: ArdentRegister) {
        try {
            register.sender.cmdSend(Jsoup.connect("http://www.fmylife.com/random")
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .ignoreContentType(true).get()
                    .getElementsByTag("p")[0].getElementsByTag("a")[0].allElements[0].text(), this, event)
        } catch (e: Exception) {
            onInvoke(event, arguments, flags, register)
        }
    }
}