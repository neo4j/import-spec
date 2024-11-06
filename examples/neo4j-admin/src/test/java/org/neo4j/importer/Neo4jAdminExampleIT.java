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
package org.neo4j.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.internal.SchemaNames;
import org.neo4j.driver.*;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.actions.ActionType;
import org.neo4j.importer.v1.actions.CypherAction;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.*;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class Neo4jAdminExampleIT {

    private static final String SHARED_FOLDER = "/admin-import/";

    @Container
    private static final GenericContainer<?> NEO4J = new Neo4jContainer<>(
                    DockerImageName.parse(System.getenv("BLEEDING_EDGE_NEO4J")).asCompatibleSubstituteFor("neo4j"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!")
            .withCreateContainerCmdModifier(cmd -> cmd.withUser("neo4j"))
            .withFileSystemBind(
                    MountableFile.forClasspathResource(SHARED_FOLDER).getFilesystemPath(),
                    "/import",
                    BindMode.READ_ONLY)
            .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));

    private static final String TARGET_DATABASE = "dvdrental";

    private Driver driver;

    @BeforeEach
    void prepare() {
        driver = GraphDatabase.driver(
                String.format("bolt://%s:%d".formatted(NEO4J.getHost(), NEO4J.getMappedPort(7687))),
                AuthTokens.basic("neo4j", "letmein!"));
        driver.verifyConnectivity();
    }

    @AfterEach
    void cleanUp() {
        driver.close();
    }

    @Test
    void runs_an_offline_import_of_dvd_rental_data_set() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();

            try (var reader = new InputStreamReader(stream)) {
                var specification = ImportSpecificationDeserializer.deserialize(reader);
                var sharedFolder = pathFor(SHARED_FOLDER);
                var neo4jAdmin = new Neo4jAdmin(sharedFolder, driver, TARGET_DATABASE);
                neo4jAdmin.copyFiles(specification);
                neo4jAdmin.executeImport(specification, NEO4J);
            }
        }

        assertSchema(driver);
        assertNodeCount(driver, "Actor", 200L);
        assertNodeCount(driver, "Category", 16L);
        assertNodeCount(driver, "Customer", 599L);
        assertNodeCount(driver, "Movie", 1000L);
        assertNodeCount(driver, "Inventory", 0L);
        assertRelationshipCount(driver, "Actor", "ACTED_IN", "Movie", 5462L);
        assertRelationshipCount(driver, "Movie", "IN_CATEGORY", "Category", 1000L);
        assertRelationshipCount(driver, "Customer", "HAS_RENTED", "Movie", 16044L);
    }

    private static void assertNodeCount(Driver driver, String label, long expectedCount) {
        var node = Cypher.node(label).named("n");
        var query = Cypher.match(node)
                .returning(Cypher.count(node.getRequiredSymbolicName()).as("count"))
                .build();
        var records = driver.executableQuery(query.getCypher())
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("count").asLong()).isEqualTo(expectedCount);
    }

    private static void assertRelationshipCount(
            Driver driver, String startLabel, String type, String endLabel, long expectedCount) {
        var startNode = Cypher.node(startLabel);
        var endNode = Cypher.node(endLabel);
        var relationship = startNode.relationshipTo(endNode, type).named("r");
        var query = Cypher.match(relationship)
                .returning(Cypher.count(relationship.getRequiredSymbolicName()).as("count"))
                .build();
        var records = driver.executableQuery(query.getCypher())
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("count").asLong()).isEqualTo(expectedCount);
    }

    private static void assertSchema(Driver driver) {
        assertNodeConstraint(driver, "NODE_KEY", "Actor", "id");
        assertNodeTypeConstraint(driver, "Actor", "first_name", "STRING");
        assertNodeTypeConstraint(driver, "Actor", "id", "INTEGER");
        assertNodeTypeConstraint(driver, "Actor", "last_name", "STRING");
        assertNodeConstraint(driver, "NODE_KEY", "Category", "id");
        assertNodeTypeConstraint(driver, "Category", "id", "INTEGER");
        assertNodeTypeConstraint(driver, "Category", "name", "STRING");
        assertNodeConstraint(driver, "UNIQUENESS", "Category", "name");
        assertNodeConstraint(driver, "NODE_KEY", "Customer", "id");
        assertNodeTypeConstraint(driver, "Customer", "creation_date", "DATE");
        assertNodeTypeConstraint(driver, "Customer", "email", "STRING");
        assertNodeTypeConstraint(driver, "Customer", "first_name", "STRING");
        assertNodeTypeConstraint(driver, "Customer", "id", "INTEGER");
        assertNodeTypeConstraint(driver, "Customer", "last_name", "STRING");
        assertNodeConstraint(driver, "UNIQUENESS", "Customer", "email");
        assertNodeConstraint(driver, "NODE_KEY", "Inventory", "id");
        assertNodeTypeConstraint(driver, "Inventory", "id", "INTEGER");
        assertNodeTypeConstraint(driver, "Inventory", "movie_id", "INTEGER");
        assertNodeConstraint(driver, "NODE_KEY", "Movie", "id");
        assertNodeTypeConstraint(driver, "Movie", "description", "STRING");
        assertNodeTypeConstraint(driver, "Movie", "id", "INTEGER");
        assertNodeTypeConstraint(driver, "Movie", "title", "STRING");
        assertRelationshipConstraint(driver, "RELATIONSHIP_KEY", "HAS_RENTED", "id");
        assertRelationshipTypeConstraint(driver, "HAS_RENTED", "id", "INTEGER");
    }

    private static void assertNodeConstraint(Driver driver, String constraintType, String label, String property) {
        var records = driver.executableQuery(
                        """
                        SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                        WHERE type = $constraintType AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] \
                        RETURN count(*) = 1 AS result""")
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .withParameters(Map.of("constraintType", constraintType, "label", label, "property", property))
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("result").asBoolean()).isTrue();
    }

    private static void assertNodeTypeConstraint(Driver driver, String label, String property, String propertyType) {
        var records = driver.executableQuery(
                        """
                        SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                        WHERE type = 'NODE_PROPERTY_TYPE' AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] AND propertyType = $propertyType \
                        RETURN count(*) = 1 AS result""")
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .withParameters(Map.of("label", label, "property", property, "propertyType", propertyType))
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("result").asBoolean()).isTrue();
    }

    private static void assertRelationshipConstraint(
            Driver driver, String constraintType, String relType, String property) {
        var records = driver.executableQuery(
                        """
                        SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                        WHERE type = $constraintType AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] \
                        RETURN count(*) = 1 AS result""")
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .withParameters(Map.of("constraintType", constraintType, "type", relType, "property", property))
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("result").asBoolean()).isTrue();
    }

    private static void assertRelationshipTypeConstraint(
            Driver driver, String relType, String property, String propertyType) {
        var records = driver.executableQuery(
                        """
                        SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                        WHERE type = 'RELATIONSHIP_PROPERTY_TYPE' AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] AND propertyType = $propertyType \
                        RETURN count(*) = 1 AS result""")
                .withConfig(QueryConfig.builder().withDatabase(TARGET_DATABASE).build())
                .withParameters(Map.of("type", relType, "property", property, "propertyType", propertyType))
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("result").asBoolean()).isTrue();
    }

    public static class ParquetSourceProvider implements SourceProvider<ParquetSource> {

        @Override
        public String supportedType() {
            return "parquet";
        }

        @Override
        public ParquetSource provide(ObjectNode objectNode) {
            return new ParquetSource(
                    objectNode.get("name").textValue(), objectNode.get("uri").textValue());
        }
    }

    public record ParquetSource(String name, String uri) implements Source {

        @Override
        public String getType() {
            return "parquet";
        }

        @Override
        public String getName() {
            return name;
        }
    }

    static class Neo4jAdmin {

        private final File sharedFolder;

        private final Driver driver;

        private final String targetDatabase;

        public Neo4jAdmin(File sharedFolder, Driver driver, String targetDatabase) {
            this.sharedFolder = sharedFolder;
            this.driver = driver;
            this.targetDatabase = targetDatabase;
        }

        public void copyFiles(ImportSpecification specification) throws Exception {
            for (Target target : specification.getTargets().getAll()) {
                switch (target) {
                    case NodeTarget nodeTarget -> {
                        copyFile(specification, nodeTarget);
                    }
                    case RelationshipTarget relationshipTarget -> {
                        copyFile(specification, relationshipTarget);
                    }
                    default -> throw new RuntimeException("unsupported target type: %s".formatted(target.getClass()));
                }
            }
        }

        public void executeImport(ImportSpecification specification, GenericContainer<?> neo4j) throws Exception {
            // run neo4j-admin database import
            var command = importCommand(specification, targetDatabase);
            var execution = neo4j.execInContainer(command);
            assertThat(execution.getExitCode())
                    .overridingErrorMessage(execution.getStderr())
                    .isZero();
            driver.executableQuery("CREATE DATABASE $name WAIT")
                    .withParameters(Map.of("name", targetDatabase))
                    .withConfig(QueryConfig.builder().withDatabase("system").build())
                    .execute();

            // run post actions
            for (Action action : specification.getActions()) {
                if (action.getType() == ActionType.CYPHER && action.getStage() == ActionStage.END) {
                    try (var session = driver.session(SessionConfig.forDatabase(targetDatabase))) {
                        session.run(((CypherAction) action).getQuery()).consume();
                    }
                }
            }

            // create schema
            for (String statement :
                    generateSchemaStatements(specification.getTargets().getAll())) {
                driver.executableQuery(statement)
                        .withConfig(QueryConfig.builder()
                                .withDatabase(targetDatabase)
                                .build())
                        .execute();
            }
        }

        private static String[] importCommand(ImportSpecification specification, String database) {
            var command = new StringBuilder();
            command.append("neo4j-admin database import full --verbose --input-type=parquet ");
            command.append(database);

            Targets targets = specification.getTargets();
            for (NodeTarget nodeTarget : targets.getNodes()) {
                command.append(" --nodes=");
                command.append(String.join(":", nodeTarget.getLabels()));
                command.append("=");
                command.append("/import/%s".formatted(fileName(nodeTarget)));
            }

            for (RelationshipTarget relationshipTarget : targets.getRelationships()) {
                command.append(" --relationships=");
                command.append(relationshipTarget.getType());
                command.append("=");
                command.append("/import/%s".formatted(fileName(relationshipTarget)));
            }

            return command.toString().split(" ");
        }

        private void copyFile(ImportSpecification specification, NodeTarget nodeTarget) throws Exception {
            var source = specification.findSourceByName(nodeTarget.getSource());
            assertThat(source).isInstanceOf(ParquetSource.class);
            File parquetFile = new File(sharedFolder, fileName(nodeTarget));
            List<String> fields = readFieldNames(source);
            Map<String, String> fieldMappings = computeFieldMappings(fields, nodeTarget);

            copyParquetSource((ParquetSource) source, parquetFile, fieldMappings);
        }

        private void copyFile(ImportSpecification specification, RelationshipTarget relationshipTarget)
                throws Exception {
            File parquetFile = new File(sharedFolder, fileName(relationshipTarget));

            var source = specification.findSourceByName(relationshipTarget.getSource());
            assertThat(source).isInstanceOf(ParquetSource.class);

            var startNodeTarget = specification.getTargets().getNodes().stream()
                    .filter(t -> t.getName().equals(relationshipTarget.getStartNodeReference()))
                    .findFirst()
                    .orElseThrow();

            var endNodeTarget = specification.getTargets().getNodes().stream()
                    .filter(t -> t.getName().equals(relationshipTarget.getEndNodeReference()))
                    .findFirst()
                    .orElseThrow();

            List<String> fields = readFieldNames(source);
            Map<String, String> fieldMappings =
                    computeFieldMappings(fields, relationshipTarget, startNodeTarget, endNodeTarget);

            copyParquetSource((ParquetSource) source, parquetFile, fieldMappings);
        }

        private void copyParquetSource(ParquetSource source, File targetFile, Map<String, String> fieldMappings)
                throws SQLException {
            try (var connection = DriverManager.getConnection("jdbc:duckdb:");
                    var statement = connection.prepareStatement(String.format(
                            "COPY (SELECT %s FROM read_parquet($1)) TO '" + targetFile.getAbsolutePath()
                                    + "' (FORMAT 'parquet', CODEC 'zstd')",
                            String.join(
                                    ", ",
                                    fieldMappings.entrySet().stream()
                                            .map(e -> String.format("%s AS \"%s\"", e.getKey(), e.getValue()))
                                            .toList())))) {

                statement.setString(1, source.uri());
                statement.execute();
            }
        }

        private static Map<String, String> computeFieldMappings(List<String> fields, NodeTarget nodeTarget) {
            var mappings = indexByField(nodeTarget.getProperties());
            var keyProperties = new HashSet<>(nodeTarget.getKeyProperties());

            for (String field : fields) {
                var property = mappings.get(field);
                if (property != null) {
                    mappings.put(
                            field,
                            String.format(
                                    "%s%s", property, keyProperties.contains(property) ? idSpaceFor(nodeTarget) : ""));
                }
            }

            return mappings;
        }

        // Skipping key properties for relationships as admin import does not support them
        private static Map<String, String> computeFieldMappings(
                List<String> fields,
                RelationshipTarget relationshipTarget,
                NodeTarget startNodeTarget,
                NodeTarget endNodeTarget) {
            var mappings = indexByField(relationshipTarget.getProperties());

            startNodeTarget.getProperties().stream()
                    .filter(m -> fields.contains(m.getSourceField()))
                    .filter(m -> startNodeTarget.getKeyProperties().contains(m.getTargetProperty()))
                    .forEach(m -> mappings.put(m.getSourceField(), startIdSpaceFor(startNodeTarget)));

            endNodeTarget.getProperties().stream()
                    .filter(m -> fields.contains(m.getSourceField()))
                    .filter(m -> endNodeTarget.getKeyProperties().contains(m.getTargetProperty()))
                    .forEach(m -> mappings.put(m.getSourceField(), endIdSpaceFor(endNodeTarget)));

            return mappings;
        }

        // 🐤
        private static List<String> readFieldNames(Source source) throws Exception {
            try (var connection = DriverManager.getConnection("jdbc:duckdb:");
                    var statement = connection.prepareStatement("DESCRIBE (SELECT * FROM read_parquet($1))")) {
                statement.setString(1, ((ParquetSource) source).uri());
                var fields = new ArrayList<String>();
                try (var results = statement.executeQuery()) {
                    while (results.next()) {
                        fields.add(results.getString(1));
                    }
                }
                return fields;
            }
        }

        private static Map<String, String> indexByField(List<PropertyMapping> properties) {
            var result = new HashMap<String, String>(properties.size());
            properties.forEach(mapping -> result.put(mapping.getSourceField(), mapping.getTargetProperty()));
            return result;
        }

        private static String fileName(Target target) {
            return "%s.parquet".formatted(target.getName());
        }

        private static String idSpaceFor(NodeTarget nodeTarget) {
            return idSpaceFor("ID", nodeTarget);
        }

        private static String startIdSpaceFor(NodeTarget nodeTarget) {
            return idSpaceFor("START_ID", nodeTarget);
        }

        private static String endIdSpaceFor(NodeTarget nodeTarget) {
            return idSpaceFor("END_ID", nodeTarget);
        }

        private static String idSpaceFor(String id, NodeTarget nodeTarget) {
            return ":%s(%s-%s)".formatted(id, nodeTarget.getName(), String.join("|", nodeTarget.getLabels()));
        }

        private List<String> generateSchemaStatements(List<? extends Target> targets) {
            return targets.stream()
                    .flatMap(target -> switch (target) {
                        case NodeTarget nodeTarget -> generateNodeSchemaStatements(nodeTarget);
                        case RelationshipTarget relationshipTarget -> generateRelationshipSchemaStatements(
                                relationshipTarget);
                        default -> Stream.empty();
                    })
                    .toList();
        }

        private Stream<String> generateNodeSchemaStatements(NodeTarget nodeTarget) {
            var schema = nodeTarget.getSchema();
            if (schema == null) {
                return Stream.empty();
            }
            var statements = new ArrayList<String>();
            statements.addAll(schema.getKeyConstraints().stream()
                    .map(constraint -> Map.entry("n", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR (%s:%s) REQUIRE (%s) IS NODE KEY"
                            .formatted(
                                    generateName(
                                            nodeTarget,
                                            "key",
                                            entry.getValue().getLabel(),
                                            entry.getValue().getProperties()),
                                    entry.getKey(),
                                    sanitize(entry.getValue().getLabel()),
                                    entry.getValue().getProperties().stream()
                                            .map(Neo4jAdmin::sanitize)
                                            .map(prop -> propertyOf(entry.getKey(), prop))
                                            .collect(Collectors.joining(","))))
                    .toList());
            statements.addAll(schema.getUniqueConstraints().stream()
                    .map(constraint -> Map.entry("n", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR (%s:%s) REQUIRE (%s) IS UNIQUE"
                            .formatted(
                                    generateName(
                                            nodeTarget,
                                            "unique",
                                            entry.getValue().getLabel(),
                                            entry.getValue().getProperties()),
                                    entry.getKey(),
                                    sanitize(entry.getValue().getLabel()),
                                    entry.getValue().getProperties().stream()
                                            .map(Neo4jAdmin::sanitize)
                                            .map(prop -> propertyOf(entry.getKey(), prop))
                                            .collect(Collectors.joining(","))))
                    .toList());
            statements.addAll(schema.getTypeConstraints().stream()
                    .map(constraint -> Map.entry("n", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR (%s:%s) REQUIRE %s IS :: %s"
                            .formatted(
                                    generateName(
                                            nodeTarget,
                                            "type",
                                            entry.getValue().getLabel(),
                                            List.of(entry.getValue().getProperty())),
                                    entry.getKey(),
                                    sanitize(entry.getValue().getLabel()),
                                    propertyOf(entry.getKey(), entry.getValue().getProperty()),
                                    propertyType(findPropertyType(
                                            nodeTarget.getProperties(),
                                            entry.getValue().getProperty()))))
                    .toList());
            return statements.stream();
        }

        private Stream<String> generateRelationshipSchemaStatements(RelationshipTarget relationshipTarget) {
            var schema = relationshipTarget.getSchema();
            if (schema == null) {
                return Stream.empty();
            }
            var statements = new ArrayList<String>();
            statements.addAll(schema.getKeyConstraints().stream()
                    .map(constraint -> Map.entry("r", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR ()-[%s:%s]-() REQUIRE (%s) IS RELATIONSHIP KEY"
                            .formatted(
                                    generateName(
                                            relationshipTarget,
                                            "key",
                                            relationshipTarget.getType(),
                                            entry.getValue().getProperties()),
                                    entry.getKey(),
                                    sanitize(relationshipTarget.getType()),
                                    entry.getValue().getProperties().stream()
                                            .map(Neo4jAdmin::sanitize)
                                            .map(prop -> propertyOf(entry.getKey(), prop))
                                            .collect(Collectors.joining(","))))
                    .toList());
            statements.addAll(schema.getUniqueConstraints().stream()
                    .map(constraint -> Map.entry("r", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR ()-[%s:%s]-() REQUIRE (%s) IS UNIQUE"
                            .formatted(
                                    generateName(
                                            relationshipTarget,
                                            "unique",
                                            relationshipTarget.getType(),
                                            entry.getValue().getProperties()),
                                    entry.getKey(),
                                    sanitize(relationshipTarget.getType()),
                                    entry.getValue().getProperties().stream()
                                            .map(Neo4jAdmin::sanitize)
                                            .map(prop -> propertyOf(entry.getKey(), prop))
                                            .collect(Collectors.joining(","))))
                    .toList());
            statements.addAll(schema.getTypeConstraints().stream()
                    .map(constraint -> Map.entry("r", constraint))
                    .map(entry -> "CREATE CONSTRAINT %s FOR ()-[%s:%s]-() REQUIRE %s IS :: %s"
                            .formatted(
                                    generateName(
                                            relationshipTarget,
                                            "type",
                                            relationshipTarget.getType(),
                                            List.of(entry.getValue().getProperty())),
                                    entry.getKey(),
                                    sanitize(relationshipTarget.getType()),
                                    propertyOf(entry.getKey(), entry.getValue().getProperty()),
                                    propertyType(findPropertyType(
                                            relationshipTarget.getProperties(),
                                            entry.getValue().getProperty()))))
                    .toList());
            return statements.stream();
        }

        private static PropertyType findPropertyType(List<PropertyMapping> mappings, String property) {
            var result = mappings.stream()
                    .filter(mapping -> mapping.getTargetProperty().equals(property))
                    .map(PropertyMapping::getTargetPropertyType)
                    .toList();
            assertThat(result).hasSize(1);
            return result.getFirst();
        }

        private static String generateName(EntityTarget target, String type, String label, List<String> properties) {
            return sanitize("%s_%s_%s_%s".formatted(target.getName(), type, label, String.join("-", properties)));
        }

        private static String propertyOf(String container, String property) {
            return "%s.%s".formatted(container, property);
        }

        private static String sanitize(String element) {
            var result = SchemaNames.sanitize(element);
            assertThat(result).isPresent();
            return result.get();
        }

        private static String propertyType(PropertyType propertyType) {
            return switch (propertyType) {
                case BOOLEAN -> "BOOLEAN";
                case BOOLEAN_ARRAY -> "LIST<BOOLEAN NOT NULL>";
                case DATE -> "DATE";
                case DATE_ARRAY -> "LIST<DATE NOT NULL>";
                case DURATION -> "DURATION";
                case DURATION_ARRAY -> "LIST<DURATION NOT NULL>";
                case FLOAT -> "FLOAT";
                case FLOAT_ARRAY -> "LIST<FLOAT NOT NULL>";
                case INTEGER -> "INTEGER";
                case INTEGER_ARRAY -> "LIST<INTEGER NOT NULL>";
                case LOCAL_DATETIME -> "LOCAL DATETIME";
                case LOCAL_DATETIME_ARRAY -> "LIST<LOCAL DATETIME NOT NULL>";
                case LOCAL_TIME -> "LOCAL TIME";
                case LOCAL_TIME_ARRAY -> "LIST<LOCAL TIME NOT NULL>";
                case POINT -> "POINT";
                case POINT_ARRAY -> "LIST<POINT NOT NULL>";
                case STRING -> "STRING";
                case STRING_ARRAY -> "LIST<STRING NOT NULL>";
                case ZONED_DATETIME -> "ZONED DATETIME";
                case ZONED_DATETIME_ARRAY -> "LIST<ZONED DATETIME NOT NULL>";
                case ZONED_TIME -> "ZONED TIME";
                case ZONED_TIME_ARRAY -> "LIST<ZONED TIME NOT NULL>";
                default -> throw new IllegalArgumentException(
                        String.format("Unsupported property type: %s", propertyType));
            };
        }
    }

    private static File pathFor(String classpath) throws Exception {
        URL localVolumeUrl = Neo4jAdminExampleIT.class.getResource(classpath);
        assertThat(localVolumeUrl).isNotNull();
        return new File(localVolumeUrl.toURI());
    }
}
