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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import script.TypeScriptTypes
import script.TypeScriptUnions
import java.io.File

/**
 * Given a generated .d.mts file replaces hard-coded enum classes
 * with string union types.
 * Note: this is a hack for union types until they are properly supported by kotlin plain js objects library
 * https://youtrack.jetbrains.com/issue/KT-55101/
 */
abstract class TypeScriptModifierTask : DefaultTask() {
    @get:InputFile
    abstract var typescriptFile: File

    @TaskAction
    fun run() {
        val unions = TypeScriptUnions()
        val types = TypeScriptTypes()

        // ConstraintType
        unions.create("ConstraintType", "ConstraintTypeJs")
        types.replace("NodeConstraintJs", "type", "string", "ConstraintTypeJs")
        types.replace("RelationshipConstraintJs", "type", "string", "ConstraintTypeJs")

        // IndexType
        unions.create("IndexType", "IndexTypeJs")
        types.replace("NodeIndexJs", "type", "string", "IndexTypeJs")
        types.replace("RelationshipIndexJs", "type", "string", "IndexTypeJs")

        // MappingMode
        unions.create("MappingMode", "MappingModeJs")
        types.replace("NodeMappingJs", "mode", "string", "MappingModeJs")

        // Neo4jType
        unions.create("Neo4jType", "Neo4jTypeJs")
        types.replace("PropertyJs", "type", "string", "Neo4jTypeJs")
        types.replace("TableFieldJs", "suggested", "string", "Neo4jTypeJs")
        types.replace("TableFieldJs", "supported", "Array<string>", "array<Neo4jTypeJs>")
        // This is the most fragile bit as any additions or changes won't be detected
        unions.rename(
            "Neo4jTypeJs",
            mapOf(
                "LIST_BOOLEAN" to "LIST<BOOLEAN>",
                "LIST_DATE" to "LIST<DATE>",
                "LIST_DURATION" to "LIST<DURATION>",
                "LIST_FLOAT32" to "LIST<FLOAT32>",
                "FLOAT64" to "FLOAT",
                "LIST_FLOAT64" to "LIST<FLOAT>",
                "LIST_INTEGER8" to "LIST<INTEGER8>",
                "LIST_INTEGER16" to "LIST<INTEGER16>",
                "LIST_INTEGER32" to "LIST<INTEGER32>",
                "INTEGER64" to "INTEGER",
                "LIST_INTEGER64" to "LIST<INTEGER>",
                "LOCAL_DATETIME" to "LOCAL DATETIME",
                "LIST_LOCAL_DATETIME" to "LIST<LOCAL DATETIME>",
                "LOCAL_TIME" to "LOCAL TIME",
                "LIST_LOCAL_TIME" to "LIST<LOCAL TIME>",
                "LIST_POINT" to "LIST<POINT>",
                "LIST_STRING" to "LIST<STRING>",
                "VECTOR_FLOAT" to "VECTOR<FLOAT>",
                "VECTOR_FLOAT32" to "VECTOR<FLOAT32>",
                "VECTOR_INTEGER" to "VECTOR<INTEGER>",
                "VECTOR_INTEGER32" to "VECTOR<INTEGER32>",
                "VECTOR_INTEGER16" to "VECTOR<INTEGER16>",
                "VECTOR_INTEGER8" to "VECTOR<INTEGER8>",
                "ZONED_DATETIME" to "ZONED DATETIME",
                "LIST_ZONED_DATETIME" to "LIST<ZONED DATETIME>",
                "ZONED_TIME" to "ZONED TIME",
                "LIST_ZONED_TIME" to "LIST<ZONED TIME>"
            )
        )
        val input = typescriptFile.readText()
        var result = unions.run(input)
        result = types.run(result)
        typescriptFile.writeText(result)
    }
}
