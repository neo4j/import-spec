package model.mapping

import model.mapping.JsMappingTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RelationshipMappingJsTest : JsMappingTest<RelationshipMapping, RelationshipMappingJs>() {

    override fun createClass() = RelationshipMapping(
        relationship = "relationshipId",
        table = "table_name",
        from = TargetMapping("from_node"),
        to = TargetMapping(label = "to_label"),
        properties = mutableMapOf("prop" to PropertyMapping("field")),
        mode = MappingMode.MERGE,
        matchLabel = "matchLabel",
        keys = mutableSetOf("key"),
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
