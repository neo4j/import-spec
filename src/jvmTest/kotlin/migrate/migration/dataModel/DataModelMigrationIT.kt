package migrate.migration.dataModel

import GraphSpec
import codec.schema.SchemaMap
import model.Type
import model.Version
import org.junit.jupiter.api.Disabled
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
        // Remove not-stored import model id - TODO maybe should store?
        expected.remove("importModelId")
        // Remove unused configurations
        expected.map("dataModel")
            .remove("configurations")
        val actual = graphSpec.configuration.format.decodeFromString(output) as SchemaMap
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

        assertEquals(
            graphSpec.configuration.format.encodeToString(expected),
            graphSpec.configuration.format.encodeToString(actual)
        )
    }
}
