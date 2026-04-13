package model.source

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.Neo4jType
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
import model.mapping.PropertyMapping
import model.mapping.TargetMapping
import model.mapping.TargetMappingJs
import model.mapping.propertyMappingJs
import model.mapping.toClass
import model.mapping.toJs
import model.toClass
import model.toJs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableFieldJsTest : JsMappingTest<TableField, TableFieldJs>() {

    override fun createClass() = TableField(
        type = "field_type",
        size = 10,
        suggested = Neo4jType.STRING,
        supported = setOf(Neo4jType.STRING, Neo4jType.INTEGER),
        extensions = mutableMapOf("key1" to StringValue("val1")),
    )

    override fun toJs(k: TableField): TableFieldJs = k.toJs()

    override fun toClass(js: TableFieldJs): TableField = js.toClass()

    override fun verifyJsObject(jsObject: TableFieldJs) {
        assertEquals("field_type", jsObject.type)
        assertEquals(10, jsObject.size)
        assertEquals("STRING", jsObject.suggested)
        assertContentEquals(arrayOf("STRING", "INTEGER"), jsObject.supported)
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
