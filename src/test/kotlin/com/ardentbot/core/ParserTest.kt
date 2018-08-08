package com.ardentbot.core

import org.junit.Assert
import org.junit.Test

internal class ParserTest {
    val parser = Parser()

    @Test
    fun parseMessage() {
        Assert.assertEquals(ParsedMessage(listOf("online"), listOf(Flag("r", "\"hi\""), Flag("d", "\"hi\""))),
                parser.parseMessage("online -r hi -d hi"))
        Assert.assertEquals(ParsedMessage(listOf("test"), listOf(Flag("r", "null"), Flag("d", "\"test\""))),
                parser.parseMessage("test -r -d test"))
        Assert.assertEquals(ParsedMessage(listOf("test"), listOf(Flag("d", "\"test\""), Flag("r", "null"))),
                parser.parseMessage("test -d test -r"))
    }

    @Test
    fun checkFlagValues() {
        println(parser.parseMessage("-r \"test\" \"test1\" \"test3")!!.flags.map { it.values })
        println(parser.parseMessage("-r test -q")!!.flags)
        println(parser.parseMessage("-r test -r test1 -r test2 -d hi -q")!!.flags.map { it.values })
        println(parser.parseMessage("-r hi -qb")!!.flags.map { it.value }.map { it == null })
    }

    @Test
    fun groupBy() {
        println(listOf("hi", "im", "hi", "bob", "im").groupBy { it })
    }
}
