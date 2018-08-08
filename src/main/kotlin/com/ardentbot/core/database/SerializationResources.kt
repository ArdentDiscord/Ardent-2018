package com.ardentbot.core.database

import com.google.gson.Gson
import com.rethinkdb.net.Cursor
import org.json.simple.JSONObject

private val gson = Gson()

fun <T> asPojo(map: HashMap<*, *>?, tClass: Class<T>): T? {
    return gson.fromJson(JSONObject.toJSONString(map), tClass)
}

fun <T> Any.queryAsArrayList(t: Class<T>): MutableList<T?> {
    val tS = mutableListOf<T?>()
    val cursor = this as Cursor<HashMap<*, *>>
    cursor.forEach { hashMap -> tS.add(asPojo(hashMap, t)) }
    cursor.close()
    return tS
}