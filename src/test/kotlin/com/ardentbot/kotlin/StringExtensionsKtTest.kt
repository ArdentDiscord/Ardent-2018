package com.ardentbot.kotlin

import org.junit.Assert
import org.junit.Test

internal class StringExtensionsKtTest {
    @Test
    fun applyToString() {
        Assert.assertEquals("h[[]]h", "h[[]]h".apply(listOf("[]")))
        Assert.assertEquals("This hi is 1, hi2 is two, hi3hi4 are hi5 and hi6hi7",
                "This [] is 1, [] is two, [][] are [] and [][]".apply(listOf("hi", "hi2", "hi3", "hi4", "hi5", "hi6", "hi7")))
        Assert.assertEquals("var1 var2", "[] []".apply(listOf("var1", "var2")))
        Assert.assertEquals("hi", "hi".apply(listOf()))
    }

    @Test
    fun remove() {
        println("the water boy".remove("the").remove("an").remove("oy"))
    }
}