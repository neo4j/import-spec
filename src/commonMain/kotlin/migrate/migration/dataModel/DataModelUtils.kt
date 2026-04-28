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
package migrate.migration.dataModel

import codec.schema.SchemaMap
import codec.schema.schemaMapOf

internal fun SchemaMap.ref() = string("\$ref").removePrefix("#")

internal fun SchemaMap.ref(key: String) = map(key).ref()

internal fun SchemaMap.id() = string("\$id")

internal fun unwrap(schema: SchemaMap): SchemaMap {
    if (schema.containsKey("dataModel")) {
        val model = schema.map("dataModel")
        schema.remove("dataModel")
        schema.remove("version")
        model.putAll(schema)
        return model
    }
    return schema
}

internal fun refOf(id: String) = schemaMapOf("\$ref" to "#${id.removePrefix("#")}")
