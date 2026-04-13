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
package migrate

import codec.schema.SchemaMap
import kotlin.js.JsExport

@JsExport
abstract class Migration(val fromType: String, val from: String, val toType: String, val to: String) {
    val fromKey: String
        get() = "$fromType:$from"
    val toKey: String
        get() = "$toType:$to"
    abstract fun migrate(schema: SchemaMap): SchemaMap
}
