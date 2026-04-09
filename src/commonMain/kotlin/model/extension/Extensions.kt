/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package model.extension

/**
 * Extensions for storing basic arbitrary data.
 * It's encouraged to use name-spaced setters when working with extensions programmatically.
 * Though we cannot prevent users from using top-level setters if they wish to.
 *
 * Complex data should be stored as an optional module within [model.GraphModel].
 */
interface Extensions {
    val extensions: MutableMap<String, ExtensionValue>

    fun contains(key: String): Boolean = extensions.containsKey(key)

    /*
        Name-spaced setters
     */

    fun set(section: String, key: String, value: String) {
        getOrPut(section)[key] = StringValue(value)
    }

    fun set(section: String, key: String, value: Boolean) {
        getOrPut(section)[key] = BooleanValue(value)
    }

    fun set(section: String, key: String, value: Int) {
        set(section, key, value.toLong())
    }

    fun set(section: String, key: String, value: Long) {
        getOrPut(section)[key] = LongValue(value)
    }

    fun set(section: String, key: String, value: Float) {
        set(section, key, value.toDouble())
    }

    fun set(section: String, key: String, value: Double) {
        getOrPut(section)[key] = DoubleValue(value)
    }

    private fun getOrPut(section: String): MutableMap<String, ExtensionValue> {
        val value = extensions[section] as? MapValue
        if (value == null) {
            val map = mutableMapOf<String, ExtensionValue>()
            extensions[section] = MapValue(map)
            return map
        }
        return value.value
    }

    /*
        Name-spaced getters
     */

    fun getString(section: String, key: String): String? = get(section)?.get(key)?.asString

    fun getBoolean(section: String, key: String): Boolean? = get(section)?.get(key)?.asBoolean

    fun getLong(section: String, key: String): Long? = get(section)?.get(key)?.asLong

    fun getInt(section: String, key: String): Int? = getLong(section, key)?.toInt()

    fun getDouble(section: String, key: String): Double? = get(section)?.get(key)?.asDouble

    fun getFloat(section: String, key: String): Float? = getDouble(section, key)?.toFloat()

    fun getList(section: String, key: String): List<ExtensionValue>? = get(section)?.get(key)?.asList

    fun getStringList(section: String, key: String): List<String>? = getList(section, key)?.mapNotNull { it.asString }

    fun getBooleanList(section: String, key: String): List<Boolean>? = getList(section, key)?.mapNotNull {
        it.asBoolean
    }

    fun getLongList(section: String, key: String): List<Long>? = getList(section, key)?.mapNotNull { it.asLong }

    fun getIntList(section: String, key: String): List<Int>? = getList(section, key)?.mapNotNull { it.asLong?.toInt() }

    fun getDoubleList(section: String, key: String): List<Double>? = getList(section, key)?.mapNotNull { it.asDouble }

    fun getFloatList(section: String, key: String): List<Float>? = getList(section, key)?.mapNotNull {
        it.asDouble?.toFloat()
    }

    fun getMap(section: String, key: String): Map<String, ExtensionValue>? = get(section)?.get(key)?.asMap

    fun getStringMap(section: String, key: String): Map<String, String>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, String>()
        for ((key, value) in map) {
            output[key] = value.asString ?: continue
        }
        return output
    }

    fun getBooleanMap(section: String, key: String): Map<String, Boolean>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, Boolean>()
        for ((key, value) in map) {
            output[key] = value.asBoolean ?: continue
        }
        return output
    }

    fun getLongMap(section: String, key: String): Map<String, Long>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, Long>()
        for ((key, value) in map) {
            output[key] = value.asLong ?: continue
        }
        return output
    }

    fun getIntMap(section: String, key: String): Map<String, Int>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, Int>()
        for ((key, value) in map) {
            output[key] = value.asLong?.toInt() ?: continue
        }
        return output
    }

    fun getDoubleMap(section: String, key: String): Map<String, Double>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, Double>()
        for ((key, value) in map) {
            output[key] = value.asDouble ?: continue
        }
        return output
    }

    fun getFloatMap(section: String, key: String): Map<String, Float>? {
        val map = getMap(section, key) ?: return null
        val output = mutableMapOf<String, Float>()
        for ((key, value) in map) {
            output[key] = value.asDouble?.toFloat() ?: continue
        }
        return output
    }

    private fun get(section: String): MutableMap<String, ExtensionValue>? = (extensions[section] as? MapValue)?.value

    /*
        Basic top-level setters
     */

    fun set(key: String, value: String) {
        extensions[key] = StringValue(value)
    }

    fun set(key: String, value: Boolean) {
        extensions[key] = BooleanValue(value)
    }

    fun set(key: String, value: Int) {
        set(key, value.toLong())
    }

    fun set(key: String, value: Long) {
        extensions[key] = LongValue(value)
    }

    fun set(key: String, value: Float) {
        set(key, value.toDouble())
    }

    fun set(key: String, value: Double) {
        extensions[key] = DoubleValue(value)
    }

    fun setList(key: String, value: List<String>) {
        extensions[key] = ListValue(value.map { StringValue(it) }.toMutableList())
    }

    fun setStringMap(key: String, value: Map<String, String>) {
        extensions[key] =
            MapValue(value.mapValues { StringValue(it.value) }.toMutableMap())
    }

    fun setIntMap(key: String, value: Map<String, Int>) {
        extensions[key] =
            MapValue(value.mapValues { LongValue(it.value.toLong()) }.toMutableMap())
    }

    /*
        Basic top-level getters
     */

    fun getString(key: String): String? = extensions[key]?.asString

    fun getBoolean(key: String): Boolean? = extensions[key]?.asBoolean

    fun getLong(key: String): Long? = extensions[key]?.asLong

    fun getInt(key: String): Int? = getLong(key)?.toInt()

    fun getDouble(key: String): Double? = extensions[key]?.asDouble

    fun getFloat(key: String): Float? = getDouble(key)?.toFloat()
}
