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
package model

import js.objects.Object
import js.objects.Record
import js.objects.toRecord
import js.string.JsStrings.toKotlinString
import kotlin.collections.set

fun <T> Record<String, T>.toMap(): Map<String, T> = buildMap {
    for (key in Object.keys(this)) {
        val value = this@toMap[key] ?: continue
        set(key.toKotlinString(), value)
    }
}

fun <T, R> Record<String, T>.associateBy(block: (String, T) -> R): Map<String, R> = buildMap {
    for (key in Object.keys(this)) {
        val value = this@associateBy[key] ?: continue
        val result = block(key, value)
        set(key.toKotlinString(), result)
    }
}

fun <T, R> Map<String, T>.associateBy(block: (String, T) -> R): Record<String, R> = mapValues { (key, value) ->
    block(key, value)
}.toRecord()

fun <T : Any, R> emptyRecord() = emptyMap<T, R>().toRecord()

fun jso(block: dynamic.() -> Unit): dynamic {
    val js = js("{}")
    block(js)
    return js
}
