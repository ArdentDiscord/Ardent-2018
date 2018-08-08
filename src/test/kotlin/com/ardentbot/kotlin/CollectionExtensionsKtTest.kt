package com.ardentbot.kotlin

import org.junit.Test

internal class CollectionExtensionsKtTest {
    @Test
    fun removeIndices() {
        val list = mutableListOf("one", "two", "three")
        list.removeIndices(0, 1)
        println(list)
    }
    @Test
    fun remove() {
        val list = listOf("one", "two", "three")
    }
}