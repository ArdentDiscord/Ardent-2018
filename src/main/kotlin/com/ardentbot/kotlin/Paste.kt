package com.ardentbot.kotlin

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

private val client = OkHttpClient()

/**
 * Credit to Kodehawa & the Mantaro team (https://github.com/Mantaro/MantaroBot/blob/814a2d10ae3e596bb1b303d895af6dbdf19b3ef9/src/main/java/net/kodehawa/mantarobot/utils/Utils.java)
 */
fun paste(toSend: String): String {
    try {
        val post = RequestBody.create(MediaType.parse("text/plain"), toSend)

        val toPost = Request.Builder()
                .url("https://hastebin.com/documents")
                .header("User-Agent", "Ardent Discord Bot")
                .header("Content-Type", "text/plain")
                .post(post)
                .build()

        val r = client.newCall(toPost).execute()
        val response = JSONObject(r.body()!!.string())
        r.close()
        return "https://hastebin.com/" + response.getString("key")
    } catch (e: Exception) {
        return "cannot post data to hastebin"
    }

}
