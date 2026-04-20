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
import model.property.Property
import model.relationship.Relationship
import model.relationship.RelationshipConstraint
import model.relationship.RelationshipIndex
import validate.Issue
import validate.Validation
import kotlin.collections.iterator
import kotlin.js.JsExport

@JsExport
interface RelationshipValidation : Validation {

    override fun validate(model: GraphModel, issues: MutableList<Issue>) {
        for ((relationshipId, relationship) in model.relationships) {
            validateRelationship(model, relationshipId, relationship, issues)
            for ((propertyId, property) in relationship.properties) {
                validateProperty(model, relationshipId, relationship, propertyId, property, issues)
            }
            for ((constraintId, constraint) in relationship.constraints) {
                validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues)
            }
            for ((indexId, index) in relationship.indexes) {
                validateIndex(model, relationshipId, relationship, indexId, index, issues)
            }
        }
    }

    fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
    }

    fun validateProperty(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        propertyId: String,
        property: Property,
        issues: MutableList<Issue>
    ) {
    }

    fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
        issues: MutableList<Issue>
    ) {
    }

    fun validateIndex(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        indexId: String,
        index: RelationshipIndex,
        issues: MutableList<Issue>
    ) {
    }
}
