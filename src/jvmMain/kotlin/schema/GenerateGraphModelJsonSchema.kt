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
package schema

import java.io.File
import kotlinx.schema.generator.json.JsonSchemaConfig
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.encodeToString
import kotlinx.serialization.json.Json
import model.GraphModel

fun main(args: Array<String>) {
    val out =
        File(args.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "go/spec.json").absoluteFile
    out.parentFile?.mkdirs()
    val generator = SerializationClassJsonSchemaGenerator(jsonSchemaConfig = JsonSchemaConfig.OpenAPI)
    val schema = generator.generateSchema(GraphModel.serializer().descriptor)
    out.writeText(schema.encodeToString(Json { prettyPrint = true }))
}
