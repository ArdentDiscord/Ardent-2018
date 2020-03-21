package com.ardentbot.core

import com.ardentbot.core.utils.PrefixNotFoundException
import com.ardentbot.kotlin.removeIndices
import net.dv8tion.jda.api.entities.Message

class Parser {
    fun parseBase(message: Message, prefixes: List<String>): String? {
        val content = message.contentRaw
        prefixes.forEach {
            if (content.startsWith(it)) {
                return content.removePrefix(it).removePrefix(" ").split(" ").getOrNull(0)
            }
        }
        return null
    }

    fun parseMessage(message: Message, prefix: String, base: String, useFlags: Boolean): ParsedMessage? = parseMessage(message.contentRaw.removePrefix(prefix).removePrefix(" ").removePrefix(base), useFlags)

    /**
     * Contains the Ardent parsing scheme, which mimics the Unix command line parser, while being easier for regular
     * 'users' to understand
     */
    fun parseMessage(stringContent: String, useFlags: Boolean = true): ParsedMessage? {
        val content = stringContent.removePrefix(" ").split(" ").toMutableList()
        if (content.isEmpty()) return null

        val arguments = mutableListOf<String>()
        val flags = mutableListOf<Flag>()

        while (content.isNotEmpty()) {
            when {
                (!content[0].startsWith("-") || !useFlags) || content[0].length <= 1 -> {
                    if (content[0].isNotEmpty() && content[0].isNotBlank()) {
                        arguments.add(content[0])
                    }
                    content.removeIndices(0)
                }
                else -> {
                    // Flags with arguments **can** be multiple characters long
                    if (content.size > 1 && !content[1].startsWith("-")) {
                        val args = mutableListOf<String>()
                        while (content.getOrNull(1)?.startsWith("-") == false) {
                            args.add(content[1])
                            content.removeIndices(1)
                        }
                        flags.add(Flag(content[0].substring(1), args.joinToString(" ")))
                        content.removeIndices(0)
                    } else {
                        content[0].substring(1).forEach { flags.add(Flag(it.toString(), null)) }
                        content.removeIndices(0)
                    }
                }
            }
        }

        val simplifiedFlags = flags.groupBy { it.key }
                .map { entry ->
                    Flag(entry.key, entry.value.map { flag ->
                        if (flag.rawValue?.startsWith("\"") == false) "\"" + flag.rawValue + "\"" else flag.rawValue
                    }.joinToString(" "))
                }
        return ParsedMessage(arguments, simplifiedFlags)
    }

    fun lookupPrefix(message: Message, prefixes: List<String>): String {
        prefixes.forEach { if (message.contentRaw.startsWith(it)) return it }
        throw PrefixNotFoundException("Prefix not found. Was looking for $prefixes inside ${message.contentRaw}")
    }
}

data class ParsedMessage(val arguments: List<String>, val flags: List<Flag>)
data class Flag(val key: String, var rawValue: String?) {
    val value: String?
        get() {
            return if (rawValue == "null" || rawValue == null) null else rawValue?.substring(1, rawValue!!.length - 1)
        }
    val values: List<String>?
        get() {
            if (rawValue == null) return null
            val quotationMarks = rawValue!!.mapIndexed { i, char -> if (char == '\"') i else null }.filterNotNull()
                    .toMutableList()
            val list = mutableListOf<String>()
            while (quotationMarks.isNotEmpty()) {
                if (quotationMarks.size == 1) {
                    list.add(rawValue!!.substring(quotationMarks[0] + 1))
                    quotationMarks.removeIndices(0)
                } else {
                    list.add(rawValue!!.substring(quotationMarks[0] + 1, quotationMarks[1]))
                    quotationMarks.removeIndices(0, 1)
                }
            }
            return list
        }
}

fun List<Flag>.get(key: String) = firstOrNull { it.key == key }