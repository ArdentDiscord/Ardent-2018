package com.ardentbot.core.utils

import java.io.File

data class Config(var url: String, val args: List<String>, var test: Boolean = false) {
    private val file = File(url)
    val values: HashMap<String, String> = hashMapOf()

    private fun load() {
        file.readLines().forEach {
            val split = it.split(" :: ")
            values[split[0]] = split[1]
        }
        test = values["test"]!!.toBoolean()
    }

    init {
        load()
    }

    fun reload() {
        values.clear()
        load()
    }

    operator fun get(key: String): String = values[key]
            ?: throw IllegalArgumentException("Unable to find key $key in configuration")
}