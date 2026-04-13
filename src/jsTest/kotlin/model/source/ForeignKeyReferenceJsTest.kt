package model.source

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

class ForeignKeyReferenceJsTest : JsMappingTest<ForeignKeyReference, ForeignKeyReferenceJs>() {

    override fun createClass() = ForeignKeyReference(
        table = "table_name",
        fields = setOf("field1", "field2"),
        extensions = mutableMapOf("key1" to StringValue("val1")),
    )

    override fun toJs(k: ForeignKeyReference): ForeignKeyReferenceJs = k.toJs()

    override fun toClass(js: ForeignKeyReferenceJs): ForeignKeyReference = js.toClass()

    override fun verifyJsObject(jsObject: ForeignKeyReferenceJs) {
        assertEquals("table_name", jsObject.table)
        assertContentEquals(arrayOf("field1", "field2"), jsObject.fields)
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
