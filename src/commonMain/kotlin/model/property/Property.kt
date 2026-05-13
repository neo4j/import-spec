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
package model.property

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import model.extension.ExtensionValue
import model.extension.Extensions
import model.property.Neo4jType
import model.type.Named
import kotlin.js.JsExport

@JsExport
@Serializable
@SerialName("Property")
data class Property(
    var type: Neo4jType = Neo4jType.ANY,
    var nullable: Boolean = false,
    var unique: Boolean = false,
    override val extensions: MutableMap<String, ExtensionValue> = mutableMapOf(),
    override var name: String? = null
) : Extensions,
    Named {
    val key: Boolean
        get() = !nullable && unique
}
