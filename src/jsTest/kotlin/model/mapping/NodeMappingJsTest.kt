package model.mapping

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.constraint.NodeConstraint
import model.constraint.NodeConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.index.RelationshipIndex
import model.index.RelationshipIndexJs
import model.index.toClass
import model.index.toJs
import model.toClass
import model.toJs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeMappingJsTest : JsMappingTest<NodeMapping, NodeMappingJs>() {

    override fun createClass() = NodeMapping(
        node = "node",
        table = "table_name",
        properties = mapOf("property" to PropertyMapping("field")),
        mode = MappingMode.CREATE,
        matchLabel = "label_name",
        keys = setOf("key1", "key2")
    )

    override fun toJs(k: NodeMapping): NodeMappingJs = k.toJs()

    override fun toClass(js: NodeMappingJs): NodeMapping = js.toClass()

    override fun verifyJsObject(jsObject: NodeMappingJs) {
        assertEquals("NodeMapping", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertJsEquals(propertyMappingJs("field"), jsObject.properties["property"])
        assertEquals("CREATE", jsObject.mode)
        assertEquals("label_name", jsObject.matchLabel)
        assertContentEquals(arrayOf("key1", "key2"), jsObject.keys)
    }

}
