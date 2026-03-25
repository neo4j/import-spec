package validate.node

import model.GraphModel
import model.Node
import model.index.NodeIndex
import validate.Issue
import validate.node.NodeValidation
import validate.Validation
import kotlin.collections.iterator

object NodeIndexesExists : NodeValidation {
    override fun validateIndex(
        model: GraphModel,
        nodeId: String,
        node: Node,
        indexId: String,
        index: NodeIndex,
        issues: MutableList<Issue>
    ) {
        for (property in index.properties) {
            if (!node.properties.containsKey(property)) {
                continue
            }
            issues.add(
                Issue(
                    code = "missing_node_index_property",
                    message = "Missing property with id '${property}' for node index '$indexId'",
                    path = "nodes.$nodeId.indexes.$indexId.properties.${property}"
                )
            )
        }
    }
}
