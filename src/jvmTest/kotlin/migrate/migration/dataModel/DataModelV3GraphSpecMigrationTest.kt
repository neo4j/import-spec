package migrate.migration.dataModel

import codec.format.JsonFormat
import codec.format.YamlFormat
import codec.schema.SchemaMap
import codec.format.Prettify
import org.junit.jupiter.api.Disabled
import resourceAsString
import java.io.File
import kotlin.test.Test

@Disabled
class DataModelV3GraphSpecMigrationTest {
    @Test
    fun `Test full spec`() {
        val input = DataModelV3GraphSpecMigrationTest::class.resourceAsString("northwind.json")
        val migration = DataModelV3GraphSpecMigration()
        val format = JsonFormat.build()

        val schema = format.decodeFromString(input) as SchemaMap
        println(schema)
        var output = migration.migrate(schema)

        println(output)
        output = Prettify.transform(output)

        val yaml = YamlFormat.build()
        println(yaml.encodeToString(output))
    }

    @Test
    fun `Convert full spec`() {
        for (input in File("/home/greg/Downloads/examples/").listFiles()!!) {
            if (input.extension != "json") {
                continue
            }
            val migration = DataModelV3GraphSpecMigration()
            val format = JsonFormat.build()

            val schema = format.decodeFromString(input.readText()) as SchemaMap
            println(schema)
            var output = migration.migrate(schema)

            println(output)
            output = Prettify.transform(output)

            val yaml = YamlFormat.build()
            input.parentFile.resolve("${input.nameWithoutExtension}.yaml").writeText(yaml.encodeToString(output))
        }
    }
}
