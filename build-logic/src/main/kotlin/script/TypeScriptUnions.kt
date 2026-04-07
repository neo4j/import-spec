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
package script

class TypeScriptUnions {
    private data class Union(val enum: String, val name: String)

    private val unions = mutableSetOf<Union>()
    private val renames = mutableMapOf<String, Map<String, String>>()

    /**
     * Rename values in a union
     */
    fun rename(union: String, map: Map<String, String>) {
        renames[union] = map
    }

    /**
     * Takes an enum class and generates a string union from it
     */
    fun create(enum: String, union: String) {
        unions.add(Union(enum, union))
    }

    fun run(text: String): String {
        val builder = StringBuilder()
        if (text.contains(MARKER)) {
            builder.append(text.substring(0, text.indexOf(MARKER)))
        } else {
            builder.appendLine(text)
        }
        if (unions.isNotEmpty()) {
            builder.appendLine(MARKER)
        }
        for (union in unions) {
            builder.appendLine(generateUnion(text, union))
        }
        return builder.toString()
    }

    /**
     * Kotlin enums generate a function `name()` which has a string union return type
     * i.e. `get name(): "ONE" | "TWO"`
     * Which we can find and reuse to declare a new string union type [Union.name]
     * `export type ExampleJs = "ONE" | "TWO"
     */
    private fun generateUnion(text: String, union: Union): String {
        var start = text.indexOf("abstract class ${union.enum} {")
        if (start == -1) {
            error("Enum ${union.enum} not found")
        }
        start += union.enum.length + 1
        val end = indexOfAtDepth(text, "}", 0, start)
        if (end == -1 || end <= start + 1) {
            error("Unexpected end of file for enum ${union.enum} index $start")
        }
        val section = text.substring(start + 1, end + 1)
        var index = indexOfAtDepth(section, "get name():", 1)
        if (index == -1) {
            error("Unable to find `get name()` for enum ${union.enum}")
        }
        index += "get name():".length + 1
        val closing = section.indexOf(";", index)
        var strings = section.substring(index, closing)
        val renames = renames[union.name] ?: emptyMap()
        for ((key, value) in renames) {
            strings = strings.replace("\"${key}\"", "\"${value}\"")
        }
        return "export type ${union.name} = $strings"
    }

    companion object {
        const val MARKER = "//auto-gen"
    }
}
