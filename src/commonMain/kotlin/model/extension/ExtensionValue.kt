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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable(with = ExtensionValueSerializer::class)
@SerialName("ExtensionValue")
sealed class ExtensionValue {
    val asString: String?
        get() = (this as? StringValue)?.value

    val asBoolean: Boolean?
        get() = (this as? BooleanValue)?.value

    val asLong: Long?
        get() = (this as? LongValue)?.value

    val asDouble: Double?
        get() = (this as? DoubleValue)?.value

    val asList: List<ExtensionValue>?
        get() = (this as? ListValue)?.value

    val asMap: Map<String, ExtensionValue>?
        get() = (this as? MapValue)?.value
}
