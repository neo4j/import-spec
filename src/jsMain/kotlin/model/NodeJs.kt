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

import js.objects.Record
import js.objects.toRecord
import kotlinx.js.JsPlainObject
import model.constraint.NodeConstraintJs
import model.constraint.toClass
import model.index.NodeIndexJs
import model.index.toClass

@JsExport
@JsPlainObject
external interface NodeJs {
    val labels: Array<String>
    val properties: Record<String, PropertyJs>
    val constraints: Record<String, NodeConstraintJs>
    val indexes: Record<String, NodeIndexJs>
    val extensions: Record<String, Any>
}

fun NodeJs.toClass(id: String): Node = Node(
    labels = labels.toSet(),
    properties = properties.associateBy { key, value -> value.toClass(id, key) },
    constraints = constraints.associateBy { _, value -> value.toClass() },
    indexes = indexes.associateBy { _, value -> value.toClass() },
    extensions = extensions.toMap()
)

fun Node.toJs(): NodeJs {
    val labels = labels.toTypedArray()
    val properties = emptyMap<String, PropertyJs>().toRecord()
    val constraints = emptyMap<String, NodeConstraintJs>().toRecord()
    val indexes = emptyMap<String, NodeIndexJs>().toRecord()
    val extensions = emptyMap<String, Any>().toRecord()
    return object : NodeJs {
        override val labels = labels
        override val properties = properties
        override val constraints = constraints
        override val indexes = indexes
        override val extensions = extensions
    }
}
