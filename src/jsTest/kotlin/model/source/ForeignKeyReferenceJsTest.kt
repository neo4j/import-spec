package model.source

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
