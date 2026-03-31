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
package validate.relationship

import model.GraphModel
import model.Relationship
import model.constraint.RelationshipConstraint
import validate.Issue

object RelationshipConstraints : RelationshipValidation {
    override fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
        issues: MutableList<Issue>
    ) {
        for (property in constraint.properties) {
            if (!relationship.properties.containsKey(property)) {
                continue
            }
            issues.add(
                Issue(
                    code = "missing_relation_constraint_property",
                    message = "Missing property with id '$property' for relationship constraint '$constraintId'",
                    path = "relationships.$relationshipId.constraints.$constraintId.properties.$property"
                )
            )
        }
    }
}
