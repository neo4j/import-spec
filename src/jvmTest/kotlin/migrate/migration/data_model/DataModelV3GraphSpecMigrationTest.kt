package migrate.migration.data_model

import codec.format.JsonFormat
import codec.format.YamlFormat
import codec.schema.SchemaMap
import resourceAsString
import kotlin.test.Test


class DataModelV3GraphSpecMigrationTest {

    @Test
    fun `Test full spec`() {
        val expected = DataModelV3GraphSpecMigrationTest::class.resourceAsString("graph-data-model-3.0.0.json")
        val migration = DataModelV3GraphSpecMigration()
        val format = JsonFormat.build()

        val schema = format.decodeFromString(expected) as SchemaMap
        val output = migration.migrate(schema)

        val yaml = YamlFormat.build()
        println(yaml.encodeToString(output))
    }
}
