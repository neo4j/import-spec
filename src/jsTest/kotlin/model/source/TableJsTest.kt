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

class TableJsTest : JsMappingTest<Table, TableJs>() {

    override fun createClass() = Table(
        source = "sourceId",
        fields = mutableMapOf("field" to TableField("varchar")),
        primaryKeys = setOf("field"),
        foreignKeys = mutableMapOf("key" to ForeignKey(setOf("key"), ForeignKeyReference("table"))),
        extensions = mutableMapOf("key1" to StringValue("val1")),
    )

    override fun toJs(k: Table): TableJs = k.toJs()

    override fun toClass(js: TableJs): Table = js.toClass()

    override fun verifyJsObject(jsObject: TableJs) {
        assertEquals("sourceId", jsObject.source)
        assertJsEquals(tableFieldJs("varchar"), jsObject.fields["field"])
        assertContentEquals(arrayOf("field"), jsObject.primaryKeys)
        assertJsEquals(foreignKeyJs(arrayOf("key"), foreignKeyReferenceJs("table")), jsObject.foreignKeys["key"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
