package model

import model.constraint.NodeConstraint
import model.constraint.nodeConstraintJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.index.NodeIndex
import model.index.nodeIndexJs
import kotlin.test.assertEquals

class NodeJsTest : JsMappingTest<Node, NodeJs>() {

    override fun createClass() = Node(
        labels = Labels("label"),
        properties = mapOf("prop" to Property(Neo4jType.STRING, name = "propertyName")),
        constraints = mapOf("constraint" to NodeConstraint("type", "label", setOf("prop"))),
        indexes = mapOf("index" to NodeIndex("type", setOf("label"), setOf("prop"))),
        extensions = mutableMapOf("key1" to StringValue("val1")),
        name = "Node Name",
    )

    override fun toJs(k: Node): NodeJs = k.toJs("nodeId")

    override fun toClass(js: NodeJs): Node = js.toClass("nodeId")

    override fun verifyJsObject(jsObject: NodeJs) {
        assertEquals("label", jsObject.labels.identifier)
        assertJsEquals(propertyJs("STRING", id = "prop", name = "propertyName"), jsObject.properties["prop"])
        assertJsEquals(nodeConstraintJs("type", "label", arrayOf("prop")), jsObject.constraints["constraint"])
        assertJsEquals(nodeIndexJs("type", arrayOf("label"), arrayOf("prop")), jsObject.indexes["index"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
        assertEquals("nodeId", jsObject.id)
    }

}
