package validate.relationship.constraint

import model.GraphModel
import model.Relationship
import model.constraint.ConstraintType
import model.constraint.RelationshipConstraint
import validate.Issue
import validate.relationship.RelationshipValidation
import validate.node.NodeConstraints

object RelationshipExistenceConstraint : RelationshipValidation {
    override fun dependsOn() = listOf(NodeConstraints)

    override fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
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
                code = "invalid_relation_exist_constraint_property_count",
                message = "Relationship existence constraint '$constraintId' must have exactly one property.",
                path = "relationships.$relationshipId.constraints.$constraintId.properties"
            )
        )
    }
}
