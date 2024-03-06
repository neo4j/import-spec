/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.importer.v1.e2e;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.sources.NamedJdbcSource;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceType;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.targets.Targets;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class AdminImportIT {

    @Container
    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!")
            .withCreateContainerCmdModifier(cmd -> cmd.withUser("neo4j"))
            .withFileSystemBind(
                    MountableFile.forClasspathResource("/e2e/admin-import/").getFilesystemPath(),
                    "/import",
                    BindMode.READ_ONLY);

    private static final String TARGET_DATABASE = "northwind";
    private static final String JDBC_POSTGRES_URL =
            "jdbc:tc:postgresql:15.5-alpine:///%s?TC_INITSCRIPT=e2e/admin-import/northwind_pg_dump.sql"
                    .formatted(TARGET_DATABASE);

    private Driver neo4jDriver;

    @BeforeEach
    void prepare() {
        neo4jDriver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        neo4jDriver.verifyConnectivity();
    }

    @AfterEach
    void cleanUp() {
        neo4jDriver.close();
    }

    /**
     * This is a **simplified** example of how to run a neo4j-import job based on a provided specification
     * This is **not** production-ready.
     * This only serves as a proof that the spec format is descriptive enough to run neo4j-admin imports.
     */
    @ParameterizedTest
    @ValueSource(strings = {"json", "yaml"})
    void runs_import(String extension) throws Exception {
        var importSpec =
                read("/e2e/admin-import/spec.%s".formatted(extension), ImportSpecificationDeserializer::deserialize);
        Map<String, Source> sources =
                importSpec.getSources().stream().collect(toMap(Source::getName, Function.identity()));
        File csvFolder = csvFolderPathFor("/e2e/admin-import");
        var targets = importSpec.getTargets();
        var nodeTargets = targets.getNodes();
        for (NodeTarget nodeTarget : nodeTargets) {
            var source = sources.get(nodeTarget.getSource());
            assertThat(source).isNotNull();
            Neo4jAdmin.writeHeader(csvFolder, nodeTarget);
            // note: source transformations are ignored, query is directly read from the source
            Neo4jAdmin.writeData(csvFolder, nodeTarget, SourceExecutor.read(source));
        }
        for (RelationshipTarget relationshipTarget : targets.getRelationships()) {
            var source = sources.get(relationshipTarget.getSource());
            assertThat(source).isNotNull();
            Neo4jAdmin.writeHeader(csvFolder, relationshipTarget, nodeTargets);
            // note: source transformations are ignored, query is directly read from the source
            Neo4jAdmin.writeData(csvFolder, relationshipTarget, nodeTargets, SourceExecutor.read(source));
        }
        // note: custom query targets are ignored for now
        var targetNeo4jDatabase = "%s-from-%s".formatted(TARGET_DATABASE, extension);

        Neo4jAdmin.executeImport(NEO4J, neo4jDriver, importSpec, targetNeo4jDatabase);

        var productCount = neo4jDriver
                .executableQuery("MATCH (p:Product) RETURN count(p) AS count")
                .withConfig(
                        QueryConfig.builder().withDatabase(targetNeo4jDatabase).build())
                .execute()
                .records();
        assertThat(productCount).hasSize(1);
        assertThat(productCount.getFirst().get("count").asLong()).isEqualTo(77L);
        var categoryCount = neo4jDriver
                .executableQuery("MATCH (c:Category) RETURN count(c) AS count")
                .withConfig(
                        QueryConfig.builder().withDatabase(targetNeo4jDatabase).build())
                .execute()
                .records();
        assertThat(categoryCount).hasSize(1);
        assertThat(categoryCount.getFirst().get("count").asLong()).isEqualTo(8L);
        var productInCategoryCount = neo4jDriver
                .executableQuery("MATCH (:Product)-[btc:BELONGS_TO_CATEGORY]->(:Category) RETURN count(btc) AS count")
                .withConfig(
                        QueryConfig.builder().withDatabase(targetNeo4jDatabase).build())
                .execute()
                .records();
        assertThat(productInCategoryCount).hasSize(1);
        assertThat(productInCategoryCount.getFirst().get("count").asLong()).isEqualTo(77L);
    }

    private File csvFolderPathFor(String classpath) throws Exception {
        URL localVolumeUrl = this.getClass().getResource(classpath);
        assertThat(localVolumeUrl).isNotNull();
        return new File(localVolumeUrl.toURI());
    }

    private <T> T read(String classpathResource, ThrowingFunction<Reader, T> fn) throws Exception {
        InputStream stream = this.getClass().getResourceAsStream(classpathResource);
        assertThat(stream).isNotNull();
        try (var reader = new InputStreamReader(stream)) {
            return fn.apply(reader);
        }
    }

    static class Neo4jAdmin {
        public static void writeHeader(File csvFolder, NodeTarget nodeTarget) throws IOException {
            File headerFile = new File(csvFolder, headerFileName(nodeTarget));
            try (var writer = new BufferedWriter(new FileWriter(headerFile))) {
                // note: this ignores synthetic properties from source transformations
                List<String> properties = sortedProperties(nodeTarget.getProperties());
                List<NodeKeyConstraint> nodeKeys = nodeTarget.getSchema().getNodeKeyConstraints();
                for (int i = 0; i < properties.size(); i++) {
                    var property = properties.get(i);
                    writer.append(property);
                    // note: this is quite inefficient
                    var matchingKeys = nodeKeys.stream()
                            .filter(key -> key.getProperties().contains(property))
                            .toList();
                    assertThat(matchingKeys).hasSizeLessThanOrEqualTo(1);
                    if (matchingKeys.size() == 1) {
                        NodeKeyConstraint key = matchingKeys.getFirst();
                        writer.append(":ID(%s)".formatted(idSpaceFor(key)));
                    }
                    if (i < properties.size() - 1) {
                        writer.append(",");
                    }
                }
            }
        }

        public static void writeData(File csvFolder, NodeTarget nodeTarget, List<Map<String, Object>> rows)
                throws Exception {
            File dataFile = new File(csvFolder, dataFileName(nodeTarget));
            try (var writer = new BufferedWriter(new FileWriter(dataFile))) {
                var propertyToFields = nodeTarget.getProperties().stream()
                        .collect(toMap(PropertyMapping::getTargetProperty, PropertyMapping::getSourceField));
                var properties = sortedProperties(nodeTarget.getProperties());
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < properties.size(); i++) {
                        String field = propertyToFields.get(properties.get(i));
                        assertThat(field).isNotNull();
                        Object value = row.get(field);
                        assertThat(value).isNotNull();
                        writer.append("\"%s\"".formatted(value.toString()));
                        if (i < properties.size() - 1) {
                            writer.append(",");
                        }
                    }
                    writer.append("\n");
                }
            }
        }

        public static void writeHeader(
                File csvFolder, RelationshipTarget relationshipTarget, List<NodeTarget> nodeTargets) throws Exception {

            var headerFile = new File(csvFolder, headerFileName(relationshipTarget));
            try (var writer = new BufferedWriter(new FileWriter(headerFile))) {
                // note: this does not support standalone start/end node spec yet
                var startNodeRef = relationshipTarget.getStartNodeReference();
                assertThat(startNodeRef).isNotEmpty();
                writer.write(":START_ID(%s),".formatted(idSpaceFor(findNodeKey(nodeTargets, startNodeRef))));
                var properties = sortedProperties(relationshipTarget.getProperties());
                for (int i = 0; i < properties.size(); i++) {
                    String property = properties.get(i);
                    writer.append(property);
                    // note: ignoring rel key constraints ...
                    // as there is no notion of ID space for relationships outside its nodes
                    if (i < properties.size() - 1) {
                        writer.append(",");
                    }
                }
                var endNodeRef = relationshipTarget.getEndNodeReference();
                assertThat(endNodeRef).isNotEmpty();
                writer.write(":END_ID(%s)".formatted(idSpaceFor(findNodeKey(nodeTargets, endNodeRef))));
            }
        }

        public static void writeData(
                File csvFolder,
                RelationshipTarget relationshipTarget,
                List<NodeTarget> nodeTargets,
                List<Map<String, Object>> rows)
                throws Exception {
            var dataFile = new File(csvFolder, dataFileName(relationshipTarget));
            try (var writer = new BufferedWriter(new FileWriter(dataFile))) {
                // start node keys
                var startNode = findTargetByName(nodeTargets, relationshipTarget.getStartNodeReference());
                var startNodeProperties = singleNodeKey(startNode).getProperties().stream()
                        .sorted()
                        .toList();
                var startNodePropertiesToFields = startNodeProperties.stream()
                        .collect(toMap(Function.identity(), keyProperty -> findMappedField(startNode, keyProperty)));
                // rel properties
                var propertyToFields = relationshipTarget.getProperties().stream()
                        .collect(toMap(PropertyMapping::getTargetProperty, PropertyMapping::getSourceField));
                var properties = sortedProperties(relationshipTarget.getProperties());
                // end node keys
                var endNode = findTargetByName(nodeTargets, relationshipTarget.getEndNodeReference());
                var endNodeProperties =
                        singleNodeKey(endNode).getProperties().stream().sorted().toList();
                var endNodePropertiesToFields = endNodeProperties.stream()
                        .collect(toMap(Function.identity(), keyProperty -> findMappedField(endNode, keyProperty)));

                for (Map<String, Object> row : rows) {
                    // start node key values
                    for (String startNodeProperty : startNodeProperties) {
                        String field = startNodePropertiesToFields.get(startNodeProperty);
                        assertThat(field).isNotNull();
                        writer.append(serializeValue(row, field));
                    }
                    writer.append(",");
                    // rel property values
                    for (String property : properties) {
                        String field = propertyToFields.get(property);
                        assertThat(field).isNotNull();
                        writer.append(serializeValue(row, field));
                        writer.append(",");
                    }
                    // end node key values
                    for (String endNodeProperty : endNodeProperties) {
                        String field = endNodePropertiesToFields.get(endNodeProperty);
                        assertThat(field).isNotNull();
                        writer.append(serializeValue(row, field));
                    }
                    writer.append("\n");
                }
            }
        }

        private static String findMappedField(NodeTarget nodeTarget, String property) {
            Optional<String> maybeField = nodeTarget.getProperties().stream()
                    .filter(mapping -> mapping.getTargetProperty().equals(property))
                    .map(PropertyMapping::getSourceField)
                    .findFirst();
            assertThat(maybeField).isPresent();
            return maybeField.get();
        }

        private static String idSpaceFor(NodeKeyConstraint key) {
            return "%s-ID".formatted(key.getLabel());
        }

        private static NodeKeyConstraint findNodeKey(List<NodeTarget> nodeTargets, String targetName) {
            var nodeTarget = findTargetByName(nodeTargets, targetName);
            return singleNodeKey(nodeTarget);
        }

        private static String serializeValue(Map<String, Object> row, String field) {
            Object value = row.get(field);
            assertThat(value).isNotNull();
            return "\"%s\"".formatted(value.toString());
        }

        private static NodeKeyConstraint singleNodeKey(NodeTarget nodeTarget) {
            var nodeKeys = nodeTarget.getSchema().getNodeKeyConstraints();
            assertThat(nodeKeys).hasSize(1);
            return nodeKeys.getFirst();
        }

        public static void executeImport(
                Neo4jContainer<?> neo4j, Driver driver, ImportSpecification importSpec, String neo4jDatabase)
                throws Exception {

            var command = importCommand(importSpec, neo4jDatabase);
            var execution = neo4j.execInContainer(command);
            assertThat(execution.getExitCode())
                    .overridingErrorMessage(execution.getStderr())
                    .isZero();
            driver.executableQuery("CREATE DATABASE $name WAIT")
                    .withParameters(Map.of("name", neo4jDatabase))
                    .withConfig(QueryConfig.builder().withDatabase("system").build())
                    .execute();
        }

        private static String[] importCommand(ImportSpecification importSpec, String database) {
            var command = new StringBuilder();
            command.append("neo4j-admin database import full ");
            Targets targets = importSpec.getTargets();
            for (NodeTarget nodeTarget : targets.getNodes()) {
                command.append("--nodes=");
                command.append(String.join(":", nodeTarget.getLabels()));
                command.append("=");
                command.append("/import/%s".formatted(headerFileName(nodeTarget)));
                command.append(",");
                command.append("/import/%s".formatted(dataFileName(nodeTarget)));
                command.append(" ");
            }
            for (RelationshipTarget relationshipTarget : targets.getRelationships()) {
                command.append("--relationships=");
                command.append(relationshipTarget.getType());
                command.append("=");
                command.append("/import/%s".formatted(headerFileName(relationshipTarget)));
                command.append(",");
                command.append("/import/%s".formatted(dataFileName(relationshipTarget)));
                command.append(" ");
            }
            command.append(database);
            return command.toString().split(" ");
        }

        private static String headerFileName(Target target) {
            return "%s.header.csv".formatted(target.getName());
        }

        private static String dataFileName(Target target) {
            return "%s.csv".formatted(target.getName());
        }

        private static NodeTarget findTargetByName(List<NodeTarget> nodeTargets, String name) {
            Optional<NodeTarget> maybeTarget = nodeTargets.stream()
                    .filter(target -> target.getName().equals(name))
                    .findFirst();
            assertThat(maybeTarget).isPresent();
            return maybeTarget.get();
        }

        private static List<String> sortedProperties(List<PropertyMapping> mappings) {
            return mappings.stream()
                    .map(PropertyMapping::getTargetProperty)
                    .sorted()
                    .toList();
        }
    }

    static class SourceExecutor {

        public static List<Map<String, Object>> read(Source source) throws Exception {
            SourceType sourceType = source.getType();
            switch (sourceType) {
                case JDBC -> {
                    var jdbcSource = (NamedJdbcSource) source;
                    var connectionSupplier = JdbcContext.connectionSupplier(jdbcSource.getDataSource());
                    return JdbcExecutor.execute(connectionSupplier, jdbcSource.getSql());
                }
                case BIGQUERY, TEXT -> throw new RuntimeException("TODO: %s unsupported for now".formatted(sourceType));
            }
            throw new RuntimeException("unrecognized type %s".formatted(sourceType));
        }
    }

    static class JdbcContext {

        public static ThrowingSupplier<Connection> connectionSupplier(String name) {
            assertThat(name).isEqualTo(TARGET_DATABASE);
            return () -> DriverManager.getConnection(JDBC_POSTGRES_URL);
        }
    }

    static class JdbcExecutor {
        // note: inefficient since it loads everything into memory
        public static List<Map<String, Object>> execute(ThrowingSupplier<Connection> connectionSupplier, String query)
                throws Exception {
            try (var connection = connectionSupplier.get();
                    var statement = connection.createStatement();
                    var results = statement.executeQuery(query)) {

                var metaData = results.getMetaData();
                var columnCount = metaData.getColumnCount();
                var columns = new ArrayList<String>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(i - 1, metaData.getColumnLabel(i));
                }
                var rows = new ArrayList<Map<String, Object>>();
                while (results.next()) {
                    var row = new HashMap<String, Object>();
                    for (String column : columns) {
                        row.put(column, results.getObject(column));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    @FunctionalInterface
    interface ThrowingFunction<I, O> {
        public O apply(I input) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        public T get() throws Exception;
    }
}
