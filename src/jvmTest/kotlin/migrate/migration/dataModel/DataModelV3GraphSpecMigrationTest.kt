package migrate.migration.dataModel

import GraphSpec
import codec.format.JsonFormat
import codec.format.YamlFormat
import codec.schema.SchemaMap
import codec.format.Prettify
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import model.GraphModel
import org.junit.jupiter.api.Disabled
import resourceAsString
import java.io.File
import kotlin.test.Test

@Disabled
class DataModelV3GraphSpecMigrationTest {
    @Test
    fun `Test full spec`() {
        val input = DataModelV3GraphSpecMigrationTest::class.resourceAsString("prod-like/northwind.json")
        val migration = DataModelV3GraphSpecMigration()
        val format = JsonFormat.build()

        val schema = format.decodeFromString(input) as SchemaMap
        var output = migration.migrate(schema)

        output = Prettify.transform(output)

        val yaml = JsonFormat.build()
        println(yaml.encodeToString(output))
    }

    @Test
    fun `Generate schema`() {
        val generator = SerializationClassJsonSchemaGenerator.Default
        val schema = generator.generateSchema(GraphModel.serializer().descriptor)
        println(schema.encodeToString(Json { prettyPrint = true }))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Generate proto schema`() {
        val schemas = ProtoBufSchemaGenerator.generateSchemaText(GraphModel.serializer().descriptor)
        println(schemas)
    }

    @Test
    fun `Test examples`() {
        val list = listOf(
            "adventureworks-sales.json",
            "chinook.json",
            "dvd-rental.json",
            "flights.json",
            "ldbc.json",
            "northwind.json",
            "pandc.json"
        )
        for (name in list) {
            val input = DataModelV3GraphSpecMigrationTest::class.resourceAsString("prod-like/$name")
            val migration = DataModelV3GraphSpecMigration()
            val format = JsonFormat.build()

            val schema = format.decodeFromString(input) as SchemaMap

            var output = migration.migrate(schema)
            output = Prettify.transform(output)

            val yaml = YamlFormat.build()
            val string = yaml.encodeToString(output)

            File("./${name.replace(".json", ".yaml")}").writeText(string)
        }
    }

    @Test
    fun `Test others`() {
        val list = listOf(
            "industry/transactions-and-account.json",
            "industry/publication-intelligence.json",
            "industry/patient-journey.json",
            // ttl
            "ttl/owl-time.json",
            "ttl/foaf.json",
            "ttl/bibo.json",
        )
        for (name in list) {
            val input = DataModelV3GraphSpecMigrationTest::class.resourceAsString(name)

            val output = GraphSpec.Json.decodeFromString(input)

            println("Decoded $output")

            val string = GraphSpec.Yaml.encodeToString(output)

            File("./${name.replace(".json", ".yaml")}").writeText(string)
        }
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
