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

data class SchemaList(val content: MutableList<SchemaElement>, override val path: String = "") :
    SchemaElement,
    MutableList<SchemaElement> by content {

    override fun repath(newPath: String) = SchemaList(
        content.mapIndexed { index, element -> element.repath("$newPath[$index]") }.toMutableList(),
        newPath
    )

    override fun equals(other: Any?): Boolean = content == other

    override fun hashCode(): Int = content.hashCode()

    override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

fun schemaListOf(vararg elements: SchemaElement): SchemaList = SchemaList(elements.toMutableList())

fun schemaListOf(vararg elements: String): SchemaList = SchemaList(elements.map { SchemaLiteral(it) }.toMutableList())
