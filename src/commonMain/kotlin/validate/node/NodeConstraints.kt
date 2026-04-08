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
package validate.node

import model.GraphModel
import model.Node
import model.constraint.NodeConstraint
import validate.Issue
import validate.Validation
import validate.node.NodeValidation
import kotlin.collections.iterator

object NodeConstraints : NodeValidation {
    override fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
        for (property in constraint.properties) {
            if (node.properties.containsKey(property)) {
                issues.add(
                    Issue(
                        code = "missing_node_constraint_property",
                        message = "Missing property with id '$property' for node constraint '$constraintId'",
                        path = "nodes.$nodeId.constraints.$constraintId.properties.$property"
                    )
                )
            }

            val label = constraint.label
            if (node.labels.implied.contains(label) || node.labels.identifier == label) {
                continue
            }
            issues.add(
                Issue(
                    code = "missing_node_constraint_label",
                    message = "Missing label with id '$label' for node constraint '$constraintId'",
                    path = "nodes.$nodeId.constraints.$constraintId.labels.$label"
                )
            )
        }
    }
}
