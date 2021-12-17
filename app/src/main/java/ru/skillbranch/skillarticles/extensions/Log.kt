package ru.skillbranch.skillarticles.extensions

import android.util.Log

/** Метод вывода в лог полного (не обрезанного) сообщения */
fun logdu(tag: String, msg: String) {
    val sb = StringBuilder(msg)
    // Штатная лог-утилита обрезает сообщение, оставляя не более 4*1024 байт
    if (sb.length > 4000) {
        val fullChunksCount = sb.length / 4000
        val total = if (sb.length % 4000 > 0) fullChunksCount + 1 else fullChunksCount
        for (i in 0 .. fullChunksCount) {
            val nextOffset = 4000 * (i + 1)
            if (nextOffset < sb.length) {
                Log.d(tag, " ~~~~~chunk " + (i + 1) + "/" + total +
                        "~~~~~: " + sb.substring(4000 * i, nextOffset))
            }
            else if (nextOffset < sb.length + 4000) {
                Log.d(tag, " ~~~~~chunk " + (i + 1) + "/" + total +
                        "~~~~~: " + sb.substring(4000 * i))
            }
        }
    } else {
        Log.d(tag, sb.toString())
    }
}



