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
                    message = "Missing property with id '${property}' for relationship constraint '$constraintId'",
                    path = "relationships.$relationshipId.constraints.$constraintId.properties.${property}"
                )
            )
        }
    }
}
