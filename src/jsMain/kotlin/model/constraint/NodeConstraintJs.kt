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
package model.constraint

import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.toMap

@JsExport
@JsPlainObject
external interface NodeConstraintJs : ConstraintJs {
    val label: String
}

fun NodeConstraint.toJs(): NodeConstraintJs = when (this) {
    is NodeExistConstraint -> nodeExistConstraintJs(
        type = ConstraintTypeJs.EXISTS,
        label = label,
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
    is NodeKeyConstraint -> nodeKeyConstraintJs(
        type = ConstraintTypeJs.KEY,
        label = label,
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
    is NodeTypeConstraint -> nodeTypeConstraintJs(
        type = ConstraintTypeJs.TYPE,
        dataType = dataType.name,
        label = label,
        properties = properties.toTypedArray()
    )
    is NodeUniqueConstraint -> nodeUniqueConstraintJs(
        type = ConstraintTypeJs.UNIQUE,
        label = label,
        properties = properties.toTypedArray(),
        options = options.toReadonlyRecord()
    )
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun NodeConstraintJs.toClass(node: String, constraint: String): NodeConstraint = when (type) {
    ConstraintTypeJs.EXISTS -> NodeExistConstraint(
        label = label,
        properties = properties.toSet(),
        options = (this as NodeExistConstraintJs).options.toMap()
    )
    ConstraintTypeJs.KEY -> NodeKeyConstraint(
        label = label,
        properties = properties.toSet(),
        options = (this as NodeKeyConstraintJs).options.toMap()
    )
    ConstraintTypeJs.TYPE -> NodeTypeConstraint(
        Neo4jType.valueOf((this as NodeTypeConstraintJs).dataType),
        label = label,
        properties = properties.toSet()
    )
    ConstraintTypeJs.UNIQUE -> NodeUniqueConstraint(
        label = label,
        properties = properties.toSet(),
        options = (this as NodeUniqueConstraintJs).options.toMap()
    )
    else -> error("Invalid node constraint type '$type' for nodes.$node.constraints.$constraint.type")
}
