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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DataModelMigrationIT {

    @TestFactory
    fun `Round trip`() =
        File(End2EndMigrationTest::class.java.getResource("prod-like")!!.path).listFiles()!!.map { file ->
            dynamicTest("dataset ${file.nameWithoutExtension}") {
                if (file.nameWithoutExtension == "two-nodes-same-name") {
                    // FIXME combining node labels
                    return@dynamicTest
                }
                val input = file.readText()
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
                // Remove unused configurations
                removeDefaultSizes(actual)
                actual = actual.map("dataModel") // Unwrap
                // Remove unused configurations that we don't mind losing
                expected.remove("configurations")
                expected
                    .map("graphMappingRepresentation")
                    .map("dataSourceSchema")
                    .listOfMaps("tableSchemas")
                    .forEach {
                        it.remove("rawType") // Shouldn't be at this level
                        it.remove("expanded")
                    }

                assertEquals(
                    graphSpec.configuration.format.encodeToString(recursiveSort(expected)),
                    graphSpec.configuration.format.encodeToString(recursiveSort(actual))
                )
            }
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
