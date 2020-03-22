package com.ardentbot.kotlin

import java.text.DateFormat
import java.text.DecimalFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone

private val formatter = DecimalFormat("#,###")

fun Long.toMinutesAndSeconds(): String {
    return timeDisplay().toString()
}

infix fun Number.withDecimalPlaceCount(places: Int): String = "%.${places}f".format(this)

fun Long.timeDisplay(): String {
    val seconds = (this / 1000) % 60
    val minutes = (this / (1000 * 60)) % 60
    val hours = (this / (1000 * 60 * 60)) % 24
    val days = (this / (1000 * 60 * 60 * 24))
    val builder = StringBuilder()
    if (days == 1.toLong()) builder.append("$days day, ")
    else if (days > 1.toLong()) builder.append("$days days, ")

    if (hours == 1.toLong()) builder.append("$hours hour, ")
    else if (hours > 1.toLong()) builder.append("$hours hours, ")

    if (minutes == 1.toLong()) builder.append("$minutes minute, ")
    else if (minutes > 1.toLong()) builder.append("$minutes minutes, ")

    if (seconds == 1.toLong()) builder.append("$minutes second")
    else builder.append("$seconds seconds")
    return builder.toString()
}

fun Number.format() = formatter.format(this)

fun Long.localeDate(): String {
    val formatter = DateFormat.getDateTimeInstance()
    formatter.timeZone = TimeZone.getTimeZone("America/Indianapolis")
    return formatter.format(Date.from(Instant.ofEpochMilli(this))) + " EDT (UTC -4)"
}