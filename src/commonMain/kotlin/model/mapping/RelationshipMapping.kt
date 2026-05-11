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
package model.mapping

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
@SerialName(MappingType.RELATIONSHIP)
data class RelationshipMapping(
    var relationship: String,
    var table: String,
    val from: TargetMapping,
    val to: TargetMapping,
    var matchLabel: String? = null,
    val properties: MutableMap<String, PropertyMapping> = mutableMapOf(),
    var mode: MappingMode = MappingMode.MERGE,
    val keys: MutableSet<String> = mutableSetOf()
) : Mapping
