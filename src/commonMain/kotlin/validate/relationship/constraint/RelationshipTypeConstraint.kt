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
package validate.relationship.constraint

import model.GraphModel
import model.Relationship
import model.constraint.ConstraintType
import model.constraint.RelationshipConstraint
import validate.Issue
import validate.node.NodeConstraints
import validate.relationship.RelationshipValidation

object RelationshipTypeConstraint : RelationshipValidation {
    override fun dependsOn() = listOf(NodeConstraints)

    override fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
        issues: MutableList<Issue>
    ) {
        if (constraint.type != ConstraintType.TYPE.name) {
            return
        }
        if (constraint.properties.size == 1) {
            return
        }
        issues.add(
            Issue(
                code = "invalid_relation_type_constraint_property_count",
                message = "Relationship type constraint '$constraintId' must have exactly one property.",
                path = "relationships.$relationshipId.constraints.$constraintId.properties"
            )
        )
    }
}
