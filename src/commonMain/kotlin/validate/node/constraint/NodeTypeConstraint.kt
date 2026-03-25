package validate.node.constraint

import model.GraphModel
import model.Node
import model.constraint.ConstraintType
import model.constraint.NodeConstraint
import validate.Issue
import validate.node.NodeValidation
import validate.node.NodeConstraints

object NodeTypeConstraint : NodeValidation {
    override fun dependsOn() = listOf(NodeConstraints)

    override fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
        if (constraint.type != ConstraintType.TYPE.name) {
            return
        }
        if (constraint.properties.size == 1) {
            return
        }
        issues.add(
            Issue(
                code = "invalid_node_type_constraint_property_count",
                message = "Node type constraint '$constraintId' must have exactly one property.",
                path = "nodes.$nodeId.constraints.$constraintId.properties"
            )
        )
    }

}
