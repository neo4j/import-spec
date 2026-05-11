package model.relationship

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import model.property.Neo4jType
import model.property.Property
import model.property.propertyJs
import kotlin.test.assertEquals

class RelationshipJsTest : JsMappingTest<Relationship, RelationshipJs>() {

    override fun createClass() = Relationship(
        type = "RELATIONSHIP_TYPE",
        from = RelationshipTarget("from_node"),
        to = RelationshipTarget("to_node"),
        properties = mutableMapOf("prop" to Property(Neo4jType.STRING, name = "property_name")),
        constraints = mutableMapOf("constraint" to RelationshipConstraint("type", mutableSetOf("prop"))),
        indexes = mutableMapOf("index" to RelationshipIndex("type", mutableSetOf("prop"))),
        extensions = mutableMapOf("key1" to StringValue("val1")),
        name = "relationshipName"
    )

    override fun toJs(k: Relationship): RelationshipJs = k.toJs("relationshipId")

    override fun toClass(js: RelationshipJs): Relationship = js.toClass("relationshipId")

    override fun verifyJsObject(jsObject: RelationshipJs) {
        assertEquals("RELATIONSHIP_TYPE", jsObject.type)
        assertEquals("from_node", jsObject.from.node)
        assertEquals("to_node", jsObject.to.node)
        assertJsEquals(propertyJs("STRING", id = "prop", name = "property_name"), jsObject.properties["prop"])
        assertJsEquals(relationshipConstraintJs("type", arrayOf("prop")), jsObject.constraints["constraint"])
        assertJsEquals(relationshipIndexJs("type", arrayOf("prop")), jsObject.indexes["index"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
        assertEquals("relationshipId", jsObject.id)
        assertEquals("relationshipName", jsObject.name)
    }

}
