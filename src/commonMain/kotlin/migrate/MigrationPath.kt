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
import model.Type

class MigrationPath(val migrations: Map<String, List<Migration>>) {

    fun migrate(schema: SchemaMap, type: String, targetVersion: String): SchemaMap {
        println("Migrate: $schema")
        val from = version(schema, type)
        val to = version(targetVersion, Type.GRAPH_SPEC)
        val path = findPath(from, to) ?: error("No migration path found between versions $from and $to")
        var map = schema
        for (migration in path) {
            map = migration.migrate(map)
            map["version"] = migration.to
        }
        return map
    }

    private fun version(version: String, type: String): String {
        var semver = version.substringBefore("-")
        if (semver.count { it == '.' } > 1) {
            semver = semver.substringBeforeLast(".") // 3.0, 2.4 etc..
        }
        if (type == Type.GRAPH_SPEC) {
            return semver
        }
        return "$type:$semver"
    }
    private fun version(schema: SchemaMap, type: String): String {
        val version = schema.stringOrNull("version") ?: error("Version must be specified")
        return version(version, type)
    }

    /**
     * Basic breath first search to find a migration path between [to] and [from] versions
     */
    fun findPath(from: String, to: String): List<Migration>? {
        if (from == to) {
            return emptyList()
        }

        val frontier = mutableMapOf<String, Migration>()
        val visited = mutableSetOf(from)
        val stack = mutableListOf(from)

        while (!stack.isEmpty()) {
            val version = stack.removeFirst()
            for (migration in migrations[version] ?: continue) {
                if (visited.contains(migration.to)) {
                    continue
                }
                visited.add(migration.to)
                frontier[migration.to] = migration
                if (migration.to == to) {
                    return path(from, to, frontier)
                }
                stack.add(migration.to)
            }
        }
        return null
    }

    private fun path(start: String, end: String, frontier: MutableMap<String, Migration>): List<Migration>? {
        val reversed = mutableListOf<Migration>()
        var cur = end
        while (cur != start) {
            val m = frontier[cur] ?: return null
            reversed.add(0, m)
            cur = m.from
        }
        return reversed
    }
}
