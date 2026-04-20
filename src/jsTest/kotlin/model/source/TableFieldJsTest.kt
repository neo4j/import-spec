package model.source

import model.mapping.JsMappingTest
import model.property.Neo4jType
import model.extension.StringValue
import model.extension.stringValueJs
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
