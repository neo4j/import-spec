package migrate.migration.dataModel

import GraphSpec
import codec.schema.SchemaElement
import codec.schema.SchemaList
import codec.schema.SchemaMap
import model.Type
import model.Version
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import resourceAsString
import kotlin.test.Test
import kotlin.test.assertEquals

class DataModelMigrationIT {
    @Test
    fun `Northwind round trip`() {
        val input = End2EndMigrationTest::class.resourceAsString("northwind-3.0.json")
        val graphModel = GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL)
        val graphSpec = GraphSpec {
            json {
                encodeDefaults = true
                prettyPrint = true
            }
        }
        val output = graphSpec.encodeToString(graphModel, Type.DATA_MODEL, Version.DATA_MODEL_V30)
        val expected = graphSpec.configuration.format.decodeFromString(input) as SchemaMap
        removeUnusedFields(expected)
        val actual = graphSpec.configuration.format.decodeFromString(output) as SchemaMap
        removeDefaultSizes(actual)

        assertEquals(
            graphSpec.configuration.format.encodeToString(expected),
            graphSpec.configuration.format.encodeToString(actual)
        )
    }

    private fun removeUnusedFields(expected: SchemaMap) {
        // Remove not-stored import model id - TODO maybe should store?
        expected.remove("importModelId")
        // Remove unused configurations
        expected.map("dataModel")
            .remove("configurations")
    }

    private fun removeDefaultSizes(actual: SchemaMap) {
        // Remove default size: -1
        actual
            .map("dataModel")
            .map("graphMappingRepresentation")
            .map("dataSourceSchema")
            .listOfMaps("tableSchemas")
            .forEach { schema ->
                schema.listOfMaps("fields").forEach {
                    it.remove("size")
                }
            }
    }

    @Disabled
    @TestFactory
    fun `Prod round trip`() = listOf("adventureworks-sales", "chinook", "dvd-rental", "flights", "ldbc", "northwind", "pandc").map { file ->
        dynamicTest("Round trip $file") {
            val input = End2EndMigrationTest::class.resourceAsString("prod-like/${file}.json")
            val graphModel = GraphSpec.Json.decodeFromString(input, Type.DATA_MODEL)
            val graphSpec = GraphSpec {
                json {
                    encodeDefaults = true
                    prettyPrint = true
                }
            }
            val output = graphSpec.encodeToString(graphModel, Type.DATA_MODEL, Version.DATA_MODEL_V30)
            val expected = graphSpec.configuration.format.decodeFromString(input) as SchemaMap
            var actual = graphSpec.configuration.format.decodeFromString(output) as SchemaMap
            println(actual)
            // Remove unused configurations
            removeDefaultSizes(actual)
            actual = actual.map("dataModel") // Unwrap
            // Remove
            expected.remove("configurations")
            expected
                .map("graphMappingRepresentation")
                .map("dataSourceSchema")
                .listOfMaps("tableSchemas")
                .forEach { it.remove("expanded") }

            assertEquals(
                graphSpec.configuration.format.encodeToString(recursiveSort(expected)),
                graphSpec.configuration.format.encodeToString(recursiveSort(actual))
            )
        }
    }

    private fun recursiveSort(element: SchemaElement): SchemaElement = when (element) {
        is SchemaMap -> {
            val sorted = sortedMapOf<String, SchemaElement>()
            for ((key, value) in element.content) {
                sorted[key] = recursiveSort(value)
            }
            SchemaMap(sorted)
        }
        is SchemaList -> SchemaList(element.content.map { recursiveSort(it) }.toMutableList())
        else -> element
    }
}
