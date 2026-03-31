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
import model.Property
import model.constraint.NodeConstraint
import model.index.NodeIndex
import validate.Issue
import validate.Validation
import kotlin.collections.iterator
import kotlin.js.JsExport

@JsExport
interface NodeValidation : Validation {
    override fun validate(model: GraphModel, issues: MutableList<Issue>) {
        for ((nodeId, node) in model.nodes) {
            validateNode(model, nodeId, node, issues)
            for ((propertyId, property) in node.properties) {
                validateProperty(model, nodeId, node, propertyId, property, issues)
            }
            for ((constraintId, constraint) in node.constraints) {
                validateConstraint(model, nodeId, node, constraintId, constraint, issues)
            }
            for ((indexId, index) in node.indexes) {
                validateIndex(model, nodeId, node, indexId, index, issues)
            }
        }
    }

    fun validateNode(model: GraphModel, nodeId: String, node: Node, issues: MutableList<Issue>) {
    }

    fun validateProperty(
        model: GraphModel,
        nodeId: String,
        node: Node,
        propertyId: String,
        property: Property,
        issues: MutableList<Issue>
    ) {
    }

    fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
    }

    fun validateIndex(
        model: GraphModel,
        nodeId: String,
        node: Node,
        indexId: String,
        index: NodeIndex,
        issues: MutableList<Issue>
    ) {
    }
}
