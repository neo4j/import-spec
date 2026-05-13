/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package model

import model.mapping.NodeMapping
import model.mapping.PropertyMapping
import model.mapping.RelationshipMapping
import model.mapping.TargetMapping
import model.node.Labels
import model.node.Node
import model.node.NodeConstraint
import model.node.NodeIndex
import model.property.Property
import model.relationship.Relationship
import model.relationship.RelationshipTarget
import model.type.ConstraintType
import model.type.IndexType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrettyTest {

    @Test
    fun `test restores node label from identifier`() {
        val model = GraphModel(
            version = "1.0",
            nodes = mutableMapOf(
                "node0" to Node(
                    name = "User",
                    labels = Labels(identifier = "UserLabel")
                )
            )
        )

        model.prettify()

        val prettyNode = model.nodes["User"]
        assertNotNull(prettyNode, "Node key should be reverted to original name 'User'")
        assertEquals("UserLabel", prettyNode.label, "Label should be restored from identifier")
        assertEquals("", prettyNode.labels.identifier, "Identifier should be cleared out")
        assertNull(prettyNode.name, "Name property should be cleared out")
    }

    @Test
    fun `test does not restore label if implied or optional labels exist`() {
        val model = GraphModel(
            version = "1.0",
            nodes = mutableMapOf(
                "node0" to Node(
                    name = "User",
                    labels = Labels(
                        identifier = "UserLabel",
                        implied = mutableSetOf("Person")
                    )
                )
            )
        )

        model.prettify()

        val node = model.nodes["User"]!!
        assertNull(node.label, "Label should remain null because implied labels exist")
        assertEquals("UserLabel", node.labels.identifier, "Identifier should remain intact")
    }

    @Test
    fun `test restores multiple IDs from constraints and indexes`() {
        val model = GraphModel(
            version = "1.0",
            nodes = mutableMapOf(
                "node0" to Node(
                    name = "User1",
                    constraints = mutableMapOf(
                        "nodeConstraint0" to
                            NodeConstraint(ConstraintType.UNIQUE, properties = mutableSetOf(), name = "c1")
                    ),
                    indexes = mutableMapOf(
                        "nodeIndex0" to NodeIndex(IndexType.TEXT, mutableSetOf(), mutableSetOf(), name = "i1")
                    )
                )
            )
        )

        model.prettify()

        assertTrue(model.nodes.containsKey("User1"))

        val node = model.nodes["User1"]!!

        assertTrue(node.constraints.containsKey("c1"), "Constraint ID should be restored")
        assertNull(node.constraints["c1"]?.name, "Constraint name should be cleared")

        assertTrue(node.indexes.containsKey("i1"), "Index ID should be restored")
        assertNull(node.indexes["i1"]?.name, "Index name should be cleared")
    }

    @Test
    fun `test translates mappings and deep properties correctly`() {
        val internalModel = GraphModel(
            version = "1.0",
            nodes = mutableMapOf(
                "node0" to Node(
                    name = "Person",
                    properties = mutableMapOf("nodeProperty0" to Property(name = "age"))
                )
            ),
            relationships = mutableMapOf(
                "relationship0" to Relationship(
                    name = "FRIENDS_WITH",
                    type = "KNOWS",
                    from = RelationshipTarget(),
                    to = RelationshipTarget(),
                    properties = mutableMapOf("relationshipProperty0" to Property(name = "since"))
                )
            ),
            mappings = mutableListOf(
                RelationshipMapping(
                    relationship = "relationship0",
                    table = "friends_table",
                    from = TargetMapping(
                        node = "node0",
                        properties = mutableMapOf("nodeProperty0" to PropertyMapping("from_age"))
                    ),
                    to = TargetMapping(
                        node = "node0"
                    ),
                    properties = mutableMapOf(
                        "relationshipProperty0" to PropertyMapping("friends_since")
                    )
                )
            )
        )

        internalModel.prettify()

        // Assert Nodes and Properties
        val prettyNode = internalModel.nodes["Person"]!!
        assertNull(prettyNode.name)
        assertTrue(prettyNode.properties.containsKey("age"))

        val prettyRel = internalModel.relationships["FRIENDS_WITH"]!!
        assertNull(prettyRel.name)
        assertTrue(prettyRel.properties.containsKey("since"))

        // Assert Mappings Deep Translation
        val relMapping = internalModel.mappings.filterIsInstance<RelationshipMapping>().first()
        assertEquals("FRIENDS_WITH", relMapping.relationship)
        assertEquals("Person", relMapping.from.node)
        assertTrue(
            relMapping.from.properties.containsKey("age"),
            "From Target property should revert to human readable"
        )
        assertTrue(relMapping.properties.containsKey("since"), "Relationship property should revert to human readable")
        assertEquals("Person", relMapping.to.node)
    }

    @Test
    fun `test ignores missing mapping references gracefully`() {
        val model = GraphModel(
            version = "1.0",
            nodes = mutableMapOf(),
            mappings = mutableListOf(
                NodeMapping(node = "node0", table = "users", properties = mutableMapOf())
            )
        )

        model.prettify()

        val mapping = model.mappings.first() as NodeMapping
        // Because "node0" isn't matched to a real node to extract its Name, it stays as is.
        assertEquals("node0", mapping.node)
    }
}
