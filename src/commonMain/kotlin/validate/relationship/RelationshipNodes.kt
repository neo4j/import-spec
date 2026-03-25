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
