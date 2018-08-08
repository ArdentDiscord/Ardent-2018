package com.ardentbot.kotlin

fun <K> MutableMap<K, Int>.increment(key: K): Int {
    val value = putIfAbsent(key, 0) ?: 0
    replace(key, value + 1)
    return value
}