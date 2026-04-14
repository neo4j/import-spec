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
package codec.schema

data class SchemaLiteral(override val string: String, override val path: String = "") : SchemaPrimitive() {

    override fun repath(newPath: String) = SchemaLiteral(string, newPath)

    override val isString: Boolean
        get() = true

    override fun toString(): String = string

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SchemaLiteral

        return string == other.string
    }

    override fun hashCode(): Int = string.hashCode()
}
