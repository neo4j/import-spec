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
package model.index

import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.toMap

@JsExport
@JsPlainObject
external interface NodeIndexJs : IndexJs {
    val labels: Array<String>
    val properties: Array<String>
}

fun NodeIndex.toJs(): NodeIndexJs = when (this) {
    is NodeFullTextIndex -> nodeFullTextIndexJs(
        type = IndexType.FULLTEXT,
        labels = labels.toTypedArray(),
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
    is NodePointIndex -> nodePointIndexJs(
        type = IndexType.POINT,
        labels = labels.toTypedArray(),
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
    is NodeRangeIndex -> nodeRangeIndexJs(
        type = IndexType.RANGE,
        labels = labels.toTypedArray(),
        properties = properties.toTypedArray()
    )
    is NodeTextIndex -> nodeTextIndexJs(
        type = IndexType.TEXT,
        labels = labels.toTypedArray(),
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
    is NodeVectorIndex -> nodeVectorIndexJs(
        type = IndexType.VECTOR,
        labels = labels.toTypedArray(),
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun NodeIndexJs.toClass(node: String, index: String): NodeIndex = when (type) {
    IndexType.FULLTEXT -> NodeFullTextIndex(
        labels = labels.toSet(),
        properties = properties.toSet(),
        options = (this as NodeFullTextIndexJs).options.toMap()
    )
    IndexType.POINT -> NodePointIndex(
        labels = labels.toSet(),
        properties = properties.toSet(),
        options = (this as NodePointIndexJs).options.toMap()
    )
    IndexType.RANGE -> NodeRangeIndex(labels = labels.toSet(), properties = properties.toSet())
    IndexType.TEXT -> NodeTextIndex(
        labels = labels.toSet(),
        properties = properties.toSet(),
        options = (this as NodeTextIndexJs).options.toMap()
    )
    IndexType.VECTOR -> NodeVectorIndex(
        labels = labels.toSet(),
        properties = properties.toSet(),
        options = (this as NodeVectorIndexJs).options.toMap()
    )
    else -> error("Invalid node index type '$type' for nodes.$node.indexes.$index.type")
}
