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
import validate.Issue

object RelationshipNodes : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        if (model.nodes.containsKey(relationship.from)) {
            issues.add(
                Issue(
                    code = "missing_relation_from_node",
                    message = "Missing node with id '${relationship.from}' for relationship '$relationshipId'",
                    path = "relationships.$relationshipId.from.${relationship.from}"
                )
            )
        }
        if (model.nodes.containsKey(relationship.to)) {
            issues.add(
                Issue(
                    code = "missing_relation_to_node",
                    message = "Missing node with id '${relationship.from}' for relationship '$relationshipId'",
                    path = "relationships.$relationshipId.to.${relationship.from}"
                )
            )
        }
    }
}
