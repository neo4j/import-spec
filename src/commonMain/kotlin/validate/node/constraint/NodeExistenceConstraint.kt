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
package validate.node.constraint

import model.GraphModel
import model.node.Node
import model.node.NodeConstraint
import model.type.ConstraintType
import validate.Issue
import validate.node.NodeConstraints
import validate.node.NodeValidation

object NodeExistenceConstraint : NodeValidation {
    override fun dependsOn() = listOf(NodeConstraints)

    override fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
        if (constraint.type != ConstraintType.EXISTS.name) {
            return
        }
        if (constraint.properties.size == 1) {
            return
        }
        issues.add(
            Issue(
                code = "invalid_node_exist_constraint_property_count",
                message = "Node existence constraint '$constraintId' must have exactly one property.",
                path = "nodes.$nodeId.constraints.$constraintId.properties"
            )
        )
    }
}
