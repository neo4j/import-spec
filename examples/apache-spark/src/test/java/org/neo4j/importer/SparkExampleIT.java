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
import java.util.Map;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
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

    @Test
    public void imports_dvd_rental_data_set() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();

            var spark = SparkSession.builder()
                    .master("local[*]")
                    .appName("SparkExample")
                    .getOrCreate();

            try (var reader = new InputStreamReader(stream)) {
                var importPipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));
                var plan = importPipeline.executionPlan();

                for (var group : plan.getGroups()) {
                    for (var stage : group.getStages()) {
                        for (var step : stage.getSteps()) {
                            // TODO: fun type checking!!!

                            switch (step) {
                                case SourceStep sourceStep -> {
                                    var parquetSource = (ParquetSource) sourceStep.source();
                                    Dataset<Row> peopleDF = spark.read().parquet(parquetSource.uri);
                                    // TODO: I want to create DFs, and mb put them in a map [sourceName -> df]
                                }
                                case NodeTargetStep nodeTargetStep -> {
                                    // TODO: I want to create a DF.write(neo4j.........)
                                }
                                case RelationshipTargetStep relationshipTargetStep -> {
                                    // TODO: I want to create a DF.write(neo4j.........)
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
        var records = driver.executableQuery(
                        """
                        SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                        WHERE type = $constraintType AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] \
                        RETURN count(*) = 1 AS result""")
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
                .withParameters(Map.of("type", relType, "property", property, "propertyType", propertyType))
                .execute()
                .records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("result").asBoolean()).isTrue();
    }

    private static void assertNodeCount(Driver driver, String label, long expectedCount) {
        var node = Cypher.node(label).named("n");
        var query = Cypher.match(node)
                .returning(Cypher.count(node.getRequiredSymbolicName()).as("count"))
                .build();
        var records = driver.executableQuery(query.getCypher()).execute().records();
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
        var records = driver.executableQuery(query.getCypher()).execute().records();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("count").asLong()).isEqualTo(expectedCount);
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
