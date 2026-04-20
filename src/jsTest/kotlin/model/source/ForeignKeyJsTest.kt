package model.source

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ForeignKeyJsTest : JsMappingTest<ForeignKey, ForeignKeyJs>() {

    override fun createClass() = ForeignKey(
        fields = setOf("field1", "field2"),
        references = ForeignKeyReference("table"),
        extensions = mutableMapOf("key1" to StringValue("val1")),
    )

    override fun toJs(k: ForeignKey): ForeignKeyJs = k.toJs()

    override fun toClass(js: ForeignKeyJs): ForeignKey = js.toClass()

    override fun verifyJsObject(jsObject: ForeignKeyJs) {
        assertContentEquals(arrayOf("field1", "field2"), jsObject.fields)
        assertEquals("table", jsObject.references.table)
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
    }

}
