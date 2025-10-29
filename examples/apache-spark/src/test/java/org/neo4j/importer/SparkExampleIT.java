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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.pipeline.ImportPipeline;
import org.neo4j.importer.v1.pipeline.NodeTargetStep;
import org.neo4j.importer.v1.pipeline.RelationshipTargetStep;
import org.neo4j.importer.v1.pipeline.SourceStep;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public class SparkExampleIT {

    @ClassRule
    public static Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    @BeforeClass
    public static void beforeClass() throws Exception {
        Assume.assumeTrue(
                "Please run `gcloud auth application-default login` and define the environment variable GOOGLE_APPLICATION_CREDENTIALS with the resulting path",
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null);
    }

    @Test
    public void imports_dvd_rental_data_set() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();

            var spark = SparkSession.builder()
                    .master("local[*]")
                    .appName("SparkExample")
                    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
                    .config("spark.hadoop.fs.gs.auth.type", "APPLICATION_DEFAULT")
                    .getOrCreate();

            try (var reader = new InputStreamReader(stream)) {
                var importPipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));
                var plan = importPipeline.executionPlan();

                var sourceDataFrames = new HashMap<String, Dataset<Row>>();
                for (var group : plan.getGroups()) {
                    for (var stage : group.getStages()) {
                        for (var step : stage.getSteps()) {
                            switch (step) {
                                case SourceStep sourceStep -> {
                                    var source = sourceStep.source();
                                    assertThat(source).isInstanceOf(ParquetSource.class);
                                    var parquetSource = (ParquetSource) source;
                                    var df = spark.read().parquet(parquetSource.uri());
                                    sourceDataFrames.put(parquetSource.name(), df);
                                }
                                case NodeTargetStep nodeTargetStep -> {
                                    var df = sourceDataFrames.get(nodeTargetStep.sourceName());
                                }
                                case RelationshipTargetStep relationshipTargetStep -> {
                                    var df = sourceDataFrames.get(relationshipTargetStep.sourceName());
                                }
                                default -> {}
                            }
                        }
                    }
                }
            }
        }

        // TODO: wait for import to complete e.g. in beam we: pipeline.run().waitUntilFinish();

        try (var driver =
                GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()))) {
            driver.verifyConnectivity();

            assertSchema(driver);
            assertNodeCount(driver, "Actor", 200L);
            assertNodeCount(driver, "Category", 16L);
            assertNodeCount(driver, "Customer", 599L);
            assertNodeCount(driver, "Movie", 1000L);
            assertNodeCount(driver, "Inventory", 0);
            assertRelationshipCount(driver, "Actor", "ACTED_IN", "Movie", 5462L);
            assertRelationshipCount(driver, "Movie", "IN_CATEGORY", "Category", 1000L);
            assertRelationshipCount(driver, "Customer", "HAS_RENTED", "Movie", 16044L);
        }
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
        try (Session session = driver.session()) {
            var result = session.run(
                    """
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                            WHERE type = $constraintType AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] \
                            RETURN count(*) = 1 AS result""",
                    Map.of("constraintType", constraintType, "label", label, "property", property));
            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertNodeTypeConstraint(Driver driver, String label, String property, String propertyType) {
        try (Session session = driver.session()) {
            var result = session.run(
                    """
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                            WHERE type = 'NODE_PROPERTY_TYPE' AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] AND propertyType = $propertyType \
                            RETURN count(*) = 1 AS result""",
                    Map.of("label", label, "property", property, "propertyType", propertyType));
            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertRelationshipConstraint(
            Driver driver, String constraintType, String relType, String property) {
        try (Session session = driver.session()) {
            var result = session.run(
                    """
                                    SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                                    WHERE type = $constraintType AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] \
                                    RETURN count(*) = 1 AS result""",
                    Map.of("constraintType", constraintType, "type", relType, "property", property));

            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertRelationshipTypeConstraint(
            Driver driver, String relType, String property, String propertyType) {
        try (Session session = driver.session()) {
            var result = session.run(
                    """
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                            WHERE type = 'RELATIONSHIP_PROPERTY_TYPE' AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] AND propertyType = $propertyType \
                            RETURN count(*) = 1 AS result""",
                    Map.of("type", relType, "property", property, "propertyType", propertyType));

            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertNodeCount(Driver driver, String label, long expectedCount) {
        try (Session session = driver.session()) {
            var query = String.format("MATCH (n:`%s`) RETURN count(n) AS count", label);
            var records = session.run(query).list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((long) records.getFirst().get("count")).isEqualTo(expectedCount);
        }
    }

    private static void assertRelationshipCount(
            Driver driver, String startLabel, String type, String endLabel, long expectedCount) {
        try (Session session = driver.session()) {
            var query = String.format(
                    "MATCH (:`%s`)-[r:`%s`]->(:`%s`) RETURN count(r) AS count", startLabel, type, endLabel);
            var records = session.run(query).list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((long) records.getFirst().get("count")).isEqualTo(expectedCount);
        }
    }

    public static class ParquetSourceProvider implements SourceProvider<ParquetSource> {

        @Override
        public String supportedType() {
            return "parquet";
        }

        @SuppressWarnings({"removal"})
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
}
