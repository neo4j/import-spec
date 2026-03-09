package migrate

import codec.schema.SchemaMap

class MigrationPath(val migrations: Map<String, List<Migration>>) {

    fun migrate(schema: SchemaMap, type: String, to: String): SchemaMap {
        val from = schema.version
        val path = findPath("$type${from}", to) ?: error("No migration path found between versions $from and $to")
        var map = schema
        for (migration in path) {
            map = migration.migrate(map)
            map["version"] = migration.to
        }
        return map
    }

    private val SchemaMap.version: String
        get() = strOrNull("version") ?: error("Version must be specified")

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

    private fun path(
        start: String,
        end: String,
        frontier: MutableMap<String, Migration>,
    ): List<Migration>? {
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
