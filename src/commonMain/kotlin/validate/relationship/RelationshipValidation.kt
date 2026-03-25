package validate.relationship

import model.GraphModel
import model.Property
import model.Relationship
import model.constraint.RelationshipConstraint
import model.index.RelationshipIndex
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
