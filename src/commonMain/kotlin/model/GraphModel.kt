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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import model.mapping.Mapping
import model.source.Table
import validate.Issue
import validate.Validation
import validate.ValidationTree
import kotlin.js.JsExport
import kotlin.js.JsStatic

@JsExport
@Serializable
@SerialName("GraphModel")
data class GraphModel(
    val version: String,
    val nodes: Map<String, Node> = emptyMap(),
    val relationships: Map<String, Relationship> = emptyMap(),
    val tables: Map<String, Table> = emptyMap(),
    val mappings: List<Mapping> = emptyList()
) {
    @JsExport.Ignore
    fun validate(validators: List<Validation>): List<Issue> {
        val tree = ValidationTree()
        tree.build(validators)
        return tree.validate(this)
    }

    companion object {
        @JsStatic
        fun validate(model: GraphModel, validators: List<Validation>) = model.validate(validators)
    }
}
