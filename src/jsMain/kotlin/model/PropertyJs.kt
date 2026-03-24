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

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface PropertyJs {
    val type: String?
    val nullable: Boolean
    val unique: Boolean
}

fun propertyJs(type: String? = null, nullable: Boolean = false, unique: Boolean = false): PropertyJs = jso {
    this.type = type
    this.nullable = nullable
    this.unique = unique
}

fun Property.toJs() = propertyJs(
    type = type?.name,
    nullable = nullable,
    unique = unique
)

fun PropertyJs.toClass(parent: String, property: String): Property {
    val type = type ?: error("Missing property type for $parent.properties.$property.type")
    val neo4jType =
        Neo4jType.entries.firstOrNull { it.name == type }
            ?: error("Invalid neo4j type '$type' for $parent.properties.$property")
    return Property(neo4jType, nullable, unique)
}
