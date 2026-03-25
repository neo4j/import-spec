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
                    message = "Missing property with id '${property}' for relationship index '$indexId'",
                    path = "relationships.$relationshipId.indexes.$indexId.properties.${property}"
                )
            )
        }
    }
}
