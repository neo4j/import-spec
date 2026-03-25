package validate

import validate.node.NodeConstraints
import validate.node.NodeIndexesExists
import validate.node.constraint.NodeExistenceConstraint
import validate.node.constraint.NodeTypeConstraint
import validate.relationship.RelationshipConstraints
import validate.relationship.RelationshipIndexes
import validate.relationship.RelationshipNodes
import validate.relationship.constraint.RelationshipExistenceConstraint
import validate.relationship.constraint.RelationshipTypeConstraint
import kotlin.js.JsExport
import kotlin.js.JsStatic

@JsExport
object Validations {
    @JsStatic
    val all: List<Validation> = listOf(
        // Nodes
        NodeConstraints,
        NodeIndexesExists,
        NodeTypeConstraint,
        NodeExistenceConstraint,

        // Relationships
        RelationshipNodes,
        RelationshipIndexes,
        RelationshipConstraints,
        RelationshipExistenceConstraint,
        RelationshipTypeConstraint,
    )
}
