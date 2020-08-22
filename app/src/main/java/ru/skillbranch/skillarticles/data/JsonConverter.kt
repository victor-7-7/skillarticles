package ru.skillbranch.skillarticles.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*

object JsonConverter {
    val moshi: Moshi = Moshi.Builder()
        // convert long timestamp to date
        .add(DateAdapter())
        // convert json to class by reflection
        .add(KotlinJsonAdapterFactory()) // lecture 11, time code 01:58:07
        .build()

    class DateAdapter {
        @FromJson
        fun fromJson(timestamp: Long) = Date(timestamp)

        @ToJson
        fun toJson(date: Date) = date.time
    }
}