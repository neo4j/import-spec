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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.targets.Targets;
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

        assertNodeCount(driver, "Actor", 200L);
        assertNodeCount(driver, "Category", 16L);
        assertNodeCount(driver, "Customer", 599L);
        assertNodeCount(driver, "Movie", 1000L);
        assertNodeCount(driver, "Inventory", 4581L);
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
                        copyFile(specification.findSourceByName(nodeTarget.getSource()), nodeTarget);
                    }
                    case RelationshipTarget relationshipTarget -> {
                        // TODO
                    }
                    default -> throw new RuntimeException("unsupported target type: %s".formatted(target.getClass()));
                }
            }
        }

        public void executeImport(ImportSpecification specification, GenericContainer<?> neo4j) throws Exception {
            var command = importCommand(specification, targetDatabase);
            var execution = neo4j.execInContainer(command);
            assertThat(execution.getExitCode())
                    .overridingErrorMessage(execution.getStderr())
                    .isZero();
            driver.executableQuery("CREATE DATABASE $name WAIT")
                    .withParameters(Map.of("name", targetDatabase))
                    .withConfig(QueryConfig.builder().withDatabase("system").build())
                    .execute();
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

            // TODO: relationship targets
            return command.toString().split(" ");
        }

        // FIXME: generate data in parquet format
        private void copyFile(Source source, NodeTarget nodeTarget) throws Exception {
            assertThat(source).isInstanceOf(ParquetSource.class);
            File parquetFile = new File(sharedFolder, fileName(nodeTarget));
            List<String> fields = readParquetHeader(source);
            Map<String, String> fieldMappings = computeFieldMappings(fields, nodeTarget);

            try (var connection = DriverManager.getConnection("jdbc:duckdb:");
                    var statement = connection.prepareStatement(String.format(
                            "COPY (SELECT %s FROM read_parquet($1)) TO '" + parquetFile.getAbsolutePath()
                                    + "' (FORMAT 'parquet', CODEC 'zstd')",
                            String.join(
                                    ", ",
                                    fieldMappings.entrySet().stream()
                                            .map(e -> String.format("%s AS \"%s\"", e.getKey(), e.getValue()))
                                            .toList())))) {

                statement.setString(1, ((ParquetSource) source).uri());
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

        // 🐤
        private static List<String> readParquetHeader(Source source) throws Exception {
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
            return ":ID(%s-%s)".formatted(nodeTarget.getName(), String.join("|", nodeTarget.getLabels()));
        }
    }

    private static File pathFor(String classpath) throws Exception {
        URL localVolumeUrl = Neo4jAdminExampleIT.class.getResource(classpath);
        assertThat(localVolumeUrl).isNotNull();
        return new File(localVolumeUrl.toURI());
    }
}
