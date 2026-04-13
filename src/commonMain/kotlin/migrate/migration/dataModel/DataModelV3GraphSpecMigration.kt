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
import codec.schema.SchemaList
import codec.schema.SchemaLiteral
import codec.schema.SchemaMap
import codec.schema.SchemaNull
import codec.schema.schemaListOf
import codec.schema.schemaMapOf
import migrate.Migration
import migrate.migration.dataModel.DataModelV2V3Migration.Companion.unwrap
import model.Type
import model.Version
import model.constraint.ConstraintType
import model.index.IndexType

class DataModelV3GraphSpecMigration :
    Migration(Type.DATA_MODEL, Version.DATA_MODEL_V30, Type.GRAPH_SPEC, Version.LATEST) {

    override fun migrate(schema: SchemaMap): SchemaMap {
        val schema = unwrap(schema)
        val graphSchema = schema.map("graphSchemaRepresentation").map("graphSchema")
        val (nodeConstraints, relationshipConstraints) = gather(graphSchema, "constraints")
        val (nodeIndexes, relationshipIndexes) = gather(graphSchema, "indexes")
        val nodes = migrateNodes(graphSchema, nodeConstraints, nodeIndexes)
        val relationships = migrateRelationships(graphSchema, relationshipConstraints, relationshipIndexes)
        val mappings = migrateMappings(schema)
        val tables = migrateTables(schema)
        val output = schemaMapOf(
            "version" to schema.literal("version"),
            "nodes" to SchemaMap(nodes),
            "relationships" to SchemaMap(relationships)
        )
        if (tables.isNotEmpty()) {
            output["tables"] = tables
        }
        if (mappings.isNotEmpty()) {
            output["mappings"] = mappings
        }
        visualisation(schema, nodes)
        return output
    }

    private fun visualisation(schema: SchemaMap, nodes: MutableMap<String, SchemaElement>) {
        val visualisation = schema.remove("visualisation") as? SchemaMap ?: return
        for (vis in visualisation.listOfMaps("nodes")) {
            val ref = vis.string("id")
            val position = vis.map("position")
            val x = position.literal("x")
            val y = position.literal("y")
            val node = nodes[ref] as? SchemaMap ?: error("Unknown node $ref")
            val extensions = node.mapOrPut("extensions")
            extensions["x"] = x
            extensions["y"] = y
        }
    }

    private fun gather(
        schema: SchemaMap,
        key: String
    ): Pair<Map<String, List<SchemaMap>>, Map<String, List<SchemaMap>>> {
        val nodeConstraints = mutableMapOf<String, MutableList<SchemaMap>>()
        val relationshipConstraints = mutableMapOf<String, MutableList<SchemaMap>>()
        val constraints = schema.listOfMapsOrNull(key) ?: return Pair(emptyMap(), emptyMap())
        for (constraint in constraints) {
            val entity = constraint.string("entityType")
            if (entity == "node") {
                val nodeLabel = constraint.map("nodeLabel").string("\$ref").removePrefix("#")
                nodeConstraints.getOrPut(nodeLabel) { mutableListOf() }.add(constraint)
            } else if (entity == "relationship") {
                val relationship = constraint.string("relationshipType")
                relationshipConstraints.getOrPut(relationship) { mutableListOf() }.add(constraint)
            }
        }
        return Pair(nodeConstraints, relationshipConstraints)
    }

    private fun migrateNodes(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaElement> {
        val nodes = mutableMapOf<String, SchemaElement>()
        val nodeLabels = schema.listOfMaps("nodeLabels")
        for (nodeObject in schema.listOfMaps("nodeObjectTypes")) {
            val nodeRef = nodeObject.string("\$id")
            val labelRefs = nodeObject.listOfMaps("labels").map { it.string("\$ref").removePrefix("#") }
            val labels = labelRefs.map { labelRef ->
                nodeLabels.firstOrNull { it.string("\$id") == labelRef } ?: error("Label $labelRef not found")
            }
            val labelTokens = labels.map { it.string("token") }
            val nodeProperties = mutableMapOf<String, SchemaMap>()
            for (label in labels) {
                convertProperties(label, nodeProperties)
            }
            val implied = labelTokens.drop(1).toTypedArray()
            val labelMap = schemaMapOf("identifier" to SchemaLiteral(labelTokens.first()))
            if (implied.isNotEmpty()) {
                labelMap["implied"] = schemaListOf(*implied)
            }
            val node = schemaMapOf(
                "labels" to labelMap
            )
            val labelRef = labelRefs.firstOrNull() // TODO loop all
            if (labelRef != null) {
                updateConstraints(constraints, labelRef, labelTokens.first(), node)
                updateIndexes(indexes, labelRef, labelTokens.first(), node)
            }
            if (nodeProperties.isNotEmpty()) {
                node["properties"] = nodeProperties
            }
            val primaryLabel = labelTokens.firstOrNull()
            if (primaryLabel != null) {
                node["name"] = primaryLabel
            }
            nodes[nodeRef] = node
        }
        return nodes
    }

    private fun updateIndexes(indexes: Map<String, List<SchemaMap>>, labelRef: String, label: String, node: SchemaMap) {
        val indices = indexes[labelRef] ?: return
        val all = schemaMapOf()
        var i = 1
        for (index in indices) {
            val name = index.string("name") // node name?
            val type = index.string("indexType")
            val properties = index.listOfMapsOrNull("properties")
            val indexType = when (type) {
                "lookup" -> IndexType.LOOKUP
                "range" -> IndexType.RANGE
                "fulltext" -> IndexType.FULLTEXT
                "point" -> IndexType.POINT
                "default", "text" -> IndexType.TEXT
                else -> error("Unknown index type: '$type' at ${index.path}.$name")
            }
            val idx = schemaMapOf()
            idx["kind"] = indexType.name
            idx["label"] = label
            if (properties != null) {
                idx["properties"] = properties.map {
                    SchemaLiteral(it.string("\$ref").removePrefix("#"))
                }
            }
            if (all.containsKey(name)) {
                all["index${i++}"] = idx
            } else {
                all[name] = idx
            }
        }
        if (all.isNotEmpty()) {
            node["indexes"] = all
        }
    }

    private fun updateConstraints(
        constraints: Map<String, List<SchemaMap>>,
        labelRef: String,
        label: String,
        node: SchemaMap
    ) {
        val constraints = constraints[labelRef] ?: return
        val all = schemaMapOf()
        var i = 1
        for (constraint in constraints) {
            val name = constraint.string("name") // node name?
            val type = constraint.string("constraintType")
            val properties = constraint.listOfMapsOrNull("properties")
            val constraintType = when (type) {
                "uniqueness" -> ConstraintType.UNIQUE
                "propertyExistence" -> ConstraintType.EXISTS
                "propertyType" -> ConstraintType.TYPE
                "key" -> ConstraintType.KEY
                else -> error("Unknown constraint type: $type at ${constraint.path}.$name")
            }
            if (properties != null && properties.size > 1) {
                if (constraintType == ConstraintType.TYPE) {
                    error("Type constraints not supported on multiple properties.")
                }
            }
            val constr = schemaMapOf()
            constr["kind"] = constraintType.name
            constr["label"] = label
            if (properties != null) {
                constr["properties"] = properties.map {
                    SchemaLiteral(it.string("\$ref").removePrefix("#"))
                }
            }
            if (all.containsKey(name)) {
                all["constraint${i++}"] = constr
            } else {
                all[name] = constr
            }
        }
        if (all.isNotEmpty()) {
            node["constraints"] = all
        }
    }

    private fun migrateRelationships(
        schema: SchemaMap,
        constraints: Map<String, List<SchemaMap>>,
        indexes: Map<String, List<SchemaMap>>
    ): MutableMap<String, SchemaElement> {
        val relationships = mutableMapOf<String, SchemaElement>()
        val relationshipTypes = schema.listOfMaps("relationshipTypes")
        for (objectType in schema
            .listOfMaps("relationshipObjectTypes")) {
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref").removePrefix("#")
            val toRef = objectType.map("to").string("\$ref").removePrefix("#")

            val relationshipType = relationshipTypes
                .firstOrNull { it.string("\$id") == typeRef }
                ?: error("RelationshipType $typeRef not found")
            val token = relationshipType.literal("token")
            val properties = mutableMapOf<String, SchemaMap>()
            convertProperties(relationshipType, properties)
            val relationship = schemaMapOf(
                "type" to token,
                "from" to SchemaLiteral(fromRef),
                "to" to SchemaLiteral(toRef),
                "properties" to SchemaMap(properties)
            )
            updateConstraints(constraints, typeRef, token.string, relationship)
            updateIndexes(indexes, typeRef, token.string, relationship)
            relationships[ref] = relationship
        }
        return relationships
    }

    private fun convertProperties(label: SchemaMap, properties: MutableMap<String, SchemaMap>) {
        for (property in label.listOfMaps("properties")) {
            val ref = property.string("\$id")
            val token = property.literalOrNull("token")
            val typeObj = property.map("type") // TODO arrays
            val type = typeObj.string("type").uppercase()
            // TODO constraints
            val map = schemaMapOf(
                "name" to (token ?: SchemaNull),
                "type" to SchemaLiteral(type)
            )
            val nullable = typeObj.literalOrNull("nullable")
            if (nullable != null) {
                map["nullable"] = nullable
            }
            properties[ref] = map
        }
    }

    private fun migrateTables(schema: SchemaMap): MutableMap<String, SchemaElement> {
        val tables = mutableMapOf<String, SchemaElement>()
        val sourceSchema = schema.map("graphMappingRepresentation").map("dataSourceSchema")
        val sourceType = sourceSchema.literalOrNull("type")
        for (table in sourceSchema.listOfMaps("tableSchemas")) {
            val fields = mutableMapOf<String, SchemaElement>()
            val name = table.string("name")
            for (field in table.listOfMaps("fields")) {
                val name = field.literal("name")
                val rawType = field.literalOrNull("rawType")
                val size = field.literalOrNull("size")
                val schemaMapOf = schemaMapOf(
                    "name" to name,
                    "rawType" to rawType,
                    "size" to size
                )
                val recommendedObject = field.mapOrNull("recommendedType")
                if (recommendedObject != null) {
                    val recommendedType = recommendedObject.string("type")
                    schemaMapOf["suggested"] = SchemaLiteral(recommendedType.uppercase())
                }
                val supportedTypes = field.listOfMapsOrNull("supportedTypes")?.map {
                    it.string("type").uppercase()
                } // FIXME array types
                if (supportedTypes != null) {
                    schemaMapOf["supported"] = schemaListOf(*supportedTypes.toTypedArray())
                }
                fields[name.string] = schemaMapOf
            }
            val primaryKeys = table.listOrNull("primaryKeys")
            val foreignKeys = mutableMapOf<String, SchemaElement>()
            val foreign = table.listOfMapsOrNull("foreignKeys")
            if (foreign != null) {
                for (foreignKey in foreign) {
                    val tableRef = foreignKey.literal("referencedTable")
                    val fields = mutableListOf<SchemaElement>()
                    val referencedFields = mutableListOf<SchemaElement>()
                    for (field in foreignKey.listOfMaps("fields")) {
                        fields.add(field.literal("field"))
                        referencedFields.add(field.literal("referencedField"))
                    }
                    foreignKeys["foreignKey${foreignKeys.size + 1}"] = schemaMapOf(
                        "fields" to SchemaList(fields),
                        "references" to schemaMapOf(
                            "table" to tableRef,
                            "fields" to SchemaList(referencedFields)
                        )
                    )
                }
            }
            val schemaMapOf = schemaMapOf(
                "source" to (sourceType ?: SchemaNull),
                "fields" to SchemaMap(fields)
            )
            if (!primaryKeys.isNullOrEmpty()) {
                schemaMapOf["primaryKeys"] = primaryKeys
            }
            if (foreignKeys.isNotEmpty()) {
                schemaMapOf["foreignKeys"] = foreignKeys
            }
            tables[name] = schemaMapOf
        }
        return tables
    }

    private fun migrateMappings(schema: SchemaMap): MutableList<SchemaElement> {
        val list = mutableListOf<SchemaElement>()
        nodeMappings(schema, list)
        relationshipMappings(schema, list)
        return list
    }

    private fun relationshipMappings(schema: SchemaMap, list: MutableList<SchemaElement>) {
        val relationships = mutableMapOf<String, Triple<String, String, String>>()
        val objectTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipObjectTypes") ?: return
        for (objectType in objectTypes) {
            val ref = objectType.string("\$id")
            val typeRef = objectType.map("type").string("\$ref").removePrefix("#")
            val fromRef = objectType.map("from").string("\$ref").removePrefix("#")
            val toRef = objectType.map("to").string("\$ref").removePrefix("#")
            relationships[ref] = Triple(fromRef, typeRef, toRef)
        }

        val relationshipTypes = schema
            .map("graphSchemaRepresentation")
            .map("graphSchema")
            .listOfMapsOrNull("relationshipTypes") ?: return
        val relationshipMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("relationshipMappings") ?: return
        for (relationshipMapping in relationshipMappings) {
            val ref = relationshipMapping.map("relationship").string("\$ref").removePrefix("#")
            val (fromRef, typeRef, toRef) = relationships[ref] ?: error("Relationship $ref not found")

            val tableName = relationshipMapping.literal("tableName")
            val fromMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("fromMappings")) {
                fromMappings[key.removePrefix("#")] = schemaMapOf("field" to value as SchemaLiteral)
            }
            val toMappings = mutableMapOf<String, SchemaElement>()
            for ((key, value) in relationshipMapping.map("toMappings")) {
                toMappings[key.removePrefix("#")] = schemaMapOf("field" to value as SchemaLiteral)
            }
            val type = relationshipTypes
                .firstOrNull { it.string("\$id") == typeRef }
                ?: error("Relationship type $typeRef not found")
            val token = type.string("token")
            val propertyMappings = relationshipMapping.listOfMaps("propertyMappings")
            val properties = migratePropertyMappings(propertyMappings)
            // ref is lost, and we know type isn't unique; so we are assuming type + property id are unique
            val map = schemaMapOf(
                "type" to SchemaLiteral(token),
                "from" to schemaMapOf(
                    "node" to SchemaLiteral(fromRef),
                    "properties" to SchemaMap(fromMappings)
                ),
                "to" to schemaMapOf(
                    "node" to SchemaLiteral(toRef),
                    "properties" to SchemaMap(toMappings)
                ),
                "table" to tableName
            )
            if (properties.isNotEmpty()) {
                map["properties"] = properties
            }
            list.add(map)
        }
    }

    private fun nodeMappings(schema: SchemaMap, list: MutableList<SchemaElement>) {
        val nodeMappings = schema
            .map("graphMappingRepresentation")
            .listOfMapsOrNull("nodeMappings") ?: return
        for (nodeMapping in nodeMappings) {
            val ref = nodeMapping.map("node").string("\$ref").removePrefix("#")
            val tableName = nodeMapping.literal("tableName")
            val propertyMappings = nodeMapping.listOfMaps("propertyMappings")
            val properties = migratePropertyMappings(propertyMappings)
            list.add(
                schemaMapOf(
                    "node" to SchemaLiteral(ref),
                    "table" to tableName,
                    "properties" to SchemaMap(properties)
                )
            )
        }
    }

    private fun migratePropertyMappings(elements: List<SchemaMap>): MutableMap<String, SchemaElement> {
        val properties = mutableMapOf<String, SchemaElement>()
        for (propertyMapping in elements) {
            val propertyRef = propertyMapping.map("property").string("\$ref").removePrefix("#")
            val fieldName = propertyMapping.literal("fieldName")
            properties[propertyRef] = schemaMapOf(
                "field" to fieldName
            )
        }
        return properties
    }
}
