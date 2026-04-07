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

class TypeScriptTypes {
    private data class Replacement(val name: String, val field: String, val type: String, val union: String)

    private val replacements = mutableSetOf<Replacement>()

    /**
     * Replaces a TypeScript classes "field: type;" with "field: union;"
     */
    fun replace(name: String, field: String, type: String, union: String) {
        replacements.add(Replacement(name, field, type, union))
    }

    fun run(text: String): String {
        var output = text
        for (clazz in replacements) {
            output = replace(output, clazz)
        }
        return output
    }

    private fun replace(text: String, replace: Replacement): String {
        val start = text.indexOf("interface ${replace.name} ")
        if (start == -1) {
            error("Unable to find interface ${replace.name}")
        }
        val field = indexOfAtDepth(text, "${replace.field}: ", 1, start)
        if (field == -1) {
            error("Unable to find field ${replace.field} in interface ${replace.name}")
        }
        val end = text.indexOf(";", field)
        val typeRange = field + replace.field.length + 2 until end
        val currentType = text.substring(typeRange)
        // Already replaced - skip
        if (currentType == replace.union) {
            return text
        }
        if (currentType != replace.type) {
            error(
                "Unexpected type '$currentType' for field '${replace.field}' in interface '${replace.name}', expected '${replace.type}'"
            )
        }
        return text.replaceRange(typeRange, replace.union)
    }
}
