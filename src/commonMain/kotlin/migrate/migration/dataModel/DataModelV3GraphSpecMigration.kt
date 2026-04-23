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

import codec.schema.SchemaElement
import codec.schema.SchemaMap
import codec.schema.schemaMapOf
import codec.schema.toNotEmpty
import migrate.Migration
import model.Type
import model.Version
import model.type.ConstraintType
import model.type.ConstraintType.EXISTS
import model.type.ConstraintType.KEY
import model.type.ConstraintType.TYPE
import model.type.ConstraintType.UNIQUE
import model.type.IndexType
import model.type.IndexType.FULLTEXT
import model.type.IndexType.LOOKUP
import model.type.IndexType.POINT
import model.type.IndexType.RANGE
import model.type.IndexType.TEXT
import model.type.IndexType.VECTOR

/**
 * 3.0 -> Graph Spec 1.0
 */
class DataModelV3GraphSpecMigration :
    Migration(
        fromType = Type.DATA_MODEL,
        from = Version.DATA_MODEL_V30,
        toType = Type.GRAPH_SPEC,
        to = Version.LATEST
    ) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val schema = unwrap(schema)
        val extensions = schema.map("graphSchemaExtensionsRepresentation").listOfMapsOrNull("nodeKeyProperties")
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val (nodeConstraints, relationshipConstraints) = gather(graphSchema, "constraints")
        val (nodeIndexes, relationshipIndexes) = gather(graphSchema, "indexes")
        val nodes = migrateNodes(graphSchema, nodeConstraints, nodeIndexes, extensions)
        return schemaMapOf(
            "version" to schema.literal("version"),
            "nodes" to nodes,
            "relationships" to migrateRelationships(graphSchema, relationshipConstraints, relationshipIndexes),
            "tables" toNotEmpty migrateTables(schema),
            "mappings" toNotEmpty nodeMappings(schema) + relationshipMappings(schema),
            "display" toNotEmpty visualisation(schema, nodes)
        )
    }

    internal fun visualisation(schema: SchemaMap, nodes: MutableMap<String, SchemaMap>): SchemaMap? {
        val visualisation = schema.remove("visualisation") as? SchemaMap ?: return null
        val display = mutableMapOf<String, SchemaMap>()
        for (vis in visualisation.listOfMaps("nodes")) {
            val ref = vis.string("id")
            nodes[ref] ?: error("Unknown node $ref")
            val position = vis.map("position")
            display[ref] = schemaMapOf(
                "x" to position.literal("x"),
                "y" to position.literal("y")
            )
        }
        return schemaMapOf("nodes" to display)
    }

    internal fun gather(
        schema: SchemaMap,
        key: String
    ): Pair<Map<String, List<SchemaMap>>, Map<String, List<SchemaMap>>> {
        val elements = schema.listOfMapsOrNull(key) ?: return Pair(emptyMap(), emptyMap())
        val (nodes, rels) = elements.partition { it.string("entityType") == "node" }
        val nodeElements = nodes.groupBy { it.ref("nodeLabel") }
        val relationshipElements = rels.groupBy { it.ref("relationshipType") }
        return Pair(nodeElements, relationshipElements)
    }

    internal fun migrateNodes(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>,
        nodeKeyProperties: List<SchemaMap>?
    ): MutableMap<String, SchemaMap> {
        val nodes = mutableMapOf<String, SchemaMap>()
        val nodeLabels = schema.listOfMaps("nodeLabels").associateBy { it.id() }
        val nodeKeys = nodeKeyProperties?.associate { nkp ->
            nkp.ref("node") to nkp.listOfMaps("keyProperties").map { it.ref() }.toSet()
        } ?: emptyMap()
        for (nodeObject in schema.listOfMaps("nodeObjectTypes")) {
            val labelRefs = nodeObject.listOfMaps("labels").map { it.ref() }
            val labels = labelRefs.map { labelRef ->
                nodeLabels[labelRef] ?: error("Label $labelRef not found")
            }
            val tokens = labels.map { it.string("token") }
            val labelRef = labelRefs.firstOrNull() // TODO loop all
            val primaryLabel = tokens.first()
            val id = nodeObject.id()
            nodes[id] = schemaMapOf(
                "labels" to schemaMapOf(
                    "identifier" to primaryLabel,
                    "implied" toNotEmpty tokens.drop(1)
                    // TODO optional
                ),
                "constraints" toNotEmpty convertConstraints(constraints, labelRef, primaryLabel),
                "indexes" toNotEmpty convertIndexes(indexes, labelRef, primaryLabel),
                "properties" toNotEmpty convertProperties(labels, nodeKeys[id] ?: emptySet()),
                "name" to tokens.firstOrNull()
            )
        }
        return nodes
    }

    internal fun convertIndexes(
        indexes: Map<String, List<SchemaMap>>,
        labelRef: String?,
        label: String
    ): Map<String, SchemaMap>? = indexes[labelRef]?.associateByUniqueName("index") { index ->
        schemaMapOf(
            "type" to indexType(index).name,
            "labels" to listOf(label),
            "properties" to index.listOfMapsOrNull("properties")?.map { it.ref() }
        )
    }

    private fun indexType(index: SchemaMap): IndexType {
        val type = index.string("indexType")
        return indexType(type) ?: error("Unknown index type: '$type' at ${index.path}.${index.string("name")}")
    }

    internal fun convertConstraints(
        constraints: Map<String, List<SchemaMap>>,
        labelRef: String?,
        label: String
    ): Map<String, SchemaMap>? = constraints[labelRef]?.associateByUniqueName("constraint") { constraint ->
        val properties = constraint.listOfMapsOrNull("properties")
        val constraintType = constraintType(constraint)
        if (properties != null && properties.size > 1 && constraintType == ConstraintType.TYPE) {
            error("Type constraints not supported on multiple properties.")
        }
        schemaMapOf(
            "type" to constraintType.name,
            "label" to label,
            "properties" toNotEmpty properties?.map { it.ref() }
        )
    }

    private fun constraintType(constraint: SchemaMap): ConstraintType {
        val type = constraint.string("constraintType")
        return constraintType(type)
            ?: error("Unknown constraint type: $type at ${constraint.path}.${constraint.string("name")}")
    }

    internal fun List<SchemaMap>.associateByUniqueName(
        prefix: String,
        transform: (SchemaMap) -> SchemaMap
    ): Map<String, SchemaMap> {
        val usedNames = mutableSetOf<String>()
        return withIndex().associate { (idx, item) ->
            val name = item.stringOrNull("name")?.takeIf { usedNames.add(it) } ?: "$prefix${idx + 1}"
            name to transform(item)
        }
    }

    internal fun migrateRelationships(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaMap> {
        val relationships = mutableMapOf<String, SchemaMap>()
        val relationshipTypes = schema.listOfMaps("relationshipTypes").associateBy { it.id() }
        for (objectType in schema.listOfMaps("relationshipObjectTypes")) {
            val typeRef = objectType.ref("type")
            val relationshipType = relationshipTypes[typeRef] ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.string("token")
            relationships[objectType.id()] = schemaMapOf(
                "type" to token,
                "from" to mapOf("node" to objectType.ref("from")),
                "to" to mapOf("node" to objectType.ref("to")),
                "properties" to convertProperties(listOf(relationshipType)),
                "constraints" toNotEmpty convertConstraints(constraints, typeRef, token),
                "indexes" toNotEmpty convertIndexes(indexes, typeRef, token)
            )
        }
        return relationships
    }

    internal fun convertProperties(
        labels: List<SchemaMap>,
        keyProperties: Set<String> = emptySet()
    ): Map<String, SchemaMap> = labels
        .flatMap { label -> label.listOfMaps("properties") }
        .associate { property ->
            val typeObj = property.map("type") // TODO arrays
            // TODO constraints
            val map = schemaMapOf(
                "name" to property.literalOrNull("token"),
                "type" to typeObj.string("type").uppercase(),
                "nullable" to typeObj.literalOrNull("nullable")
            )
            val id = property.id()
            if (keyProperties.contains(id)) {
                map["nullable"] = false
                map["unique"] = true
            }
            id to map
        }

    internal fun relationshipMappings(schema: SchemaMap): List<SchemaMap> {
        val graph = schema.map("graphSchemaRepresentation").map("graphSchema")
        val relInfo = graph.listOfMapsOrNull("relationshipObjectTypes")?.associateBy { it.id() } ?: return emptyList()
        val relTypes = graph.listOfMapsOrNull("relationshipTypes")?.associateBy { it.id() } ?: return emptyList()
        val relationshipMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (mapping in relationshipMappings) {
            val ref = mapping.ref("relationship")
            val obj = relInfo[ref] ?: error("Relationship $ref not found")
            val typeRef = obj.ref("type")
            val type = relTypes[typeRef] ?: error("Relationship type $typeRef not found")
            // ref is lost, and we know type isn't unique; so we are assuming type + property id are unique
            mappings += schemaMapOf(
                "relationship" to type.string("token"),
                "from" to mapOf(
                    "node" to obj.ref("from"),
                    "properties" to mapping.entityMap("fromMappings")
                ),
                "to" to mapOf(
                    "node" to obj.ref("to"),
                    "properties" to mapping.entityMap("toMappings")
                ),
                "table" to mapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(mapping)
            )
        }
        return mappings
    }

    internal fun SchemaMap.entityMap(key: String) = map(key).map { (key, value) ->
        key.removePrefix("#") to schemaMapOf("field" to value)
    }.toMap()

    internal fun nodeMappings(schema: SchemaMap): List<SchemaMap> {
        val nodeMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("nodeMappings") ?: return emptyList()
        val mappings = mutableListOf<SchemaMap>()
        for (nodeMapping in nodeMappings) {
            mappings += schemaMapOf(
                "node" to nodeMapping.ref("node"),
                "table" to nodeMapping.literal("tableName"),
                "properties" toNotEmpty migratePropertyMappings(nodeMapping)
            )
        }
        return mappings
    }

    private fun migratePropertyMappings(entity: SchemaMap) = entity
        .listOfMaps("propertyMappings")
        .associate { mapping ->
            mapping.ref("property") to mapOf(
                "field" to mapping.literal("fieldName")
            )
        }

    internal fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaMap> {
        val tables = mutableMapOf<String, SchemaMap>()
        val sourceSchema = schema.map("graphMappingRepresentation").map("dataSourceSchema")
        for (table in sourceSchema.listOfMaps("tableSchemas")) {
            val name = table.string("name")
            tables[name] = schemaMapOf(
                "source" to sourceSchema.literalOrNull("type"),
                "fields" to migrateFields(table),
                "primaryKeys" toNotEmpty table.listOrNull("primaryKeys"),
                "foreignKeys" toNotEmpty migrateForeignKeys(table)
            )
        }
        return tables
    }

    private fun migrateForeignKeys(table: SchemaMap): MutableMap<String, SchemaElement>? {
        val foreign = table.listOfMapsOrNull("foreignKeys") ?: return null
        val foreignKeys = mutableMapOf<String, SchemaElement>()
        for (foreignKey in foreign) {
            val fields = foreignKey.listOfMaps("fields").map { it.string("field") }
            val referencedFields = foreignKey.listOfMaps("fields").map { it.string("referencedField") }
            foreignKeys["foreignKey${foreignKeys.size + 1}"] = schemaMapOf(
                "fields" to fields,
                "references" to mapOf(
                    "table" to foreignKey.literal("referencedTable"),
                    "fields" to referencedFields
                )
            )
        }
        return foreignKeys
    }

    private fun migrateFields(table: SchemaMap): MutableMap<String, SchemaElement> {
        val fields = mutableMapOf<String, SchemaElement>()
        for (field in table.listOfMaps("fields")) {
            val name = field.string("name")
            fields[name] = schemaMapOf(
                "name" to field.literal("name"),
                "type" to field.literalOrNull("rawType"),
                "size" to field.literalOrNull("size"),
                "suggested" to field.mapOrNull("recommendedType")?.string("type")?.uppercase(),
                "supported" to field.listOfMapsOrNull("supportedTypes")?.map {
                    // FIXME array types
                    it.string("type").uppercase()
                }
            )
        }
        return fields
    }

    companion object {
        private fun indexType(name: String): IndexType? = when (name) {
            "lookup" -> LOOKUP
            "range" -> RANGE
            "fulltext" -> FULLTEXT
            "point" -> POINT
            "default", "text" -> TEXT
            "vector" -> VECTOR
            else -> null
        }

        private fun constraintType(name: String): ConstraintType? = when (name) {
            "uniqueness" -> UNIQUE
            "propertyExistence" -> EXISTS
            "propertyType" -> TYPE
            "key" -> KEY
            else -> null
        }
    }
}
