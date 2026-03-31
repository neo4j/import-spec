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
import model.index.RelationshipIndex
import validate.Issue

object RelationshipIndexes : RelationshipValidation {
    override fun validateIndex(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        indexId: String,
        index: RelationshipIndex,
        issues: MutableList<Issue>
    ) {
        for (property in index.properties) {
            if (!relationship.properties.containsKey(property)) {
                continue
            }
            issues.add(
                Issue(
                    code = "missing_relation_index_property",
                    message = "Missing property with id '$property' for relationship index '$indexId'",
                    path = "relationships.$relationshipId.indexes.$indexId.properties.$property"
                )
            )
        }
    }
}
