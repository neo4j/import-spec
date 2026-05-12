package model.relationship

import model.mapping.JsMappingTest
import model.extension.StringValue
import model.extension.stringValueJs
import model.property.Neo4jType
import model.property.Property
import model.property.propertyJs
import model.type.ConstraintType
import model.type.IndexType
import kotlin.test.assertEquals

class RelationshipJsTest : JsMappingTest<Relationship, RelationshipJs>() {

    override fun createClass() = Relationship(
        type = "RELATIONSHIP_TYPE",
        from = RelationshipTarget("from_node"),
        to = RelationshipTarget("to_node"),
        properties = mutableMapOf("prop" to Property(Neo4jType.STRING, name = "property_name")),
        constraints = mutableMapOf("constraint" to RelationshipConstraint(ConstraintType.KEY, mutableSetOf("prop"))),
        indexes = mutableMapOf("index" to RelationshipIndex(IndexType.POINT, mutableSetOf("prop"))),
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
        assertJsEquals(relationshipConstraintJs("KEY", arrayOf("prop")), jsObject.constraints["constraint"])
        assertJsEquals(relationshipIndexJs("POINT", arrayOf("prop")), jsObject.indexes["index"])
        assertJsEquals(stringValueJs("val1"), jsObject.extensions["key1"])
        assertEquals("relationshipId", jsObject.id)
        assertEquals("relationshipName", jsObject.name)
    }

}
