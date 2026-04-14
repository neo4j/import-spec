package model

import model.extension.StringValue
import model.extension.stringValueJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyJsTest : JsMappingTest<Property, PropertyJs>() {

    override fun createClass() = Property(
        type = Neo4jType.BOOLEAN,
        nullable = true,
        unique = true,
        extensions = mutableMapOf("key1" to StringValue("val1")),
        name = "propertyName"
    )

    override fun toJs(k: Property): PropertyJs = k.toJs("propertyId")

    override fun toClass(js: PropertyJs): Property = js.toClass("parent", "propertyId")

    override fun verifyJsObject(jsObject: PropertyJs) {
        assertTrue(jsObject.nullable)
        assertTrue(jsObject.unique)
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
        assertEquals("propertyId", jsObject.id)
        assertEquals("propertyName", jsObject.name)
    }

}
