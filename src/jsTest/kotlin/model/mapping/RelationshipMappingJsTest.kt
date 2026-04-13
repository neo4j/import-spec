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

class RelationshipMappingJsTest : JsMappingTest<RelationshipMapping, RelationshipMappingJs>() {

    override fun createClass() = RelationshipMapping(
        relationship = "relationshipId",
        table = "table_name",
        from = TargetMapping("from_node"),
        to = TargetMapping(label = "to_label"),
        properties = mapOf("prop" to PropertyMapping("field")),
        mode = MappingMode.MERGE,
        matchLabel = "matchLabel",
        keys = setOf("key"),
    )

    override fun toJs(k: RelationshipMapping): RelationshipMappingJs = k.toJs()

    override fun toClass(js: RelationshipMappingJs): RelationshipMapping = js.toClass()

    override fun verifyJsObject(jsObject: RelationshipMappingJs) {
        assertEquals("RelationshipMapping", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertEquals("from_node", jsObject.from.node)
        assertEquals("to_label", jsObject.to.label)
        assertJsEquals(propertyMappingJs("field"), jsObject.properties["prop"])
        assertEquals("MERGE", jsObject.mode)
        assertEquals("matchLabel", jsObject.matchLabel)
        assertContentEquals(arrayOf("key"), jsObject.keys)
    }

}
