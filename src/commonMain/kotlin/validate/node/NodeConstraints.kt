package validate.node

import model.GraphModel
import model.Node
import model.constraint.NodeConstraint
import validate.Issue
import validate.node.NodeValidation
import validate.Validation
import kotlin.collections.iterator

object NodeConstraints : NodeValidation {
    override fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
        for (property in constraint.properties) {
            if (node.properties.containsKey(property)) {
                issues.add(
                    Issue(
                        code = "missing_node_constraint_property",
                        message = "Missing property with id '${property}' for node constraint '$constraintId'",
                        path = "nodes.$nodeId.constraints.$constraintId.properties.${property}"
                    )
                )
            }
            for (label in node.labels) {
                if (!node.labels.contains(label)) {
                    continue
                }
                issues.add(
                    Issue(
                        code = "missing_node_constraint_label",
                        message = "Missing label with id '${label}' for node constraint '$constraintId'",
                        path = "nodes.$nodeId.constraints.$constraintId.labels.${label}"
                    )
                )
            }
        }
    }
}
