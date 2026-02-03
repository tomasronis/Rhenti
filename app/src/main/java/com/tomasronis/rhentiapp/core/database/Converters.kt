package com.tomasronis.rhentiapp.core.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Type converters for Room database.
 * Handles conversion of complex types to/from database-compatible formats.
 */
class Converters {

    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Int>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
    )

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String? {
        return value?.let { mapAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>? {
        return value?.let { mapAdapter.fromJson(it) }
    }
}
