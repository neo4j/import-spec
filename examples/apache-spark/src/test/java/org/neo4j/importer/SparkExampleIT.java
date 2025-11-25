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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.internal.SchemaNames;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.pipeline.ActionStep;
import org.neo4j.importer.v1.pipeline.EntityTargetStep;
import org.neo4j.importer.v1.pipeline.ImportExecutionPlan;
import org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepGroup;
import org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepStage;
import org.neo4j.importer.v1.pipeline.ImportPipeline;
import org.neo4j.importer.v1.pipeline.NodeTargetStep;
import org.neo4j.importer.v1.pipeline.RelationshipTargetStep;
import org.neo4j.importer.v1.pipeline.SourceStep;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.NodeMatchMode;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.WriteMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SparkExampleIT {

    private static final String NEO4J_DATASOURCE = "org.neo4j.spark.DataSource";

    private static SparkSession spark = null;

    @Container
    public static Neo4jContainer NEO4J = new Neo4jContainer(DockerImageName.parse("neo4j:5-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    @BeforeAll
    public static void beforeClass() {
        Assumptions.assumeTrue(
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null,
                "Please run `gcloud auth application-default login` and define the environment variable GOOGLE_APPLICATION_CREDENTIALS with the resulting path");

        spark = SparkSession.builder()
                .master("local[*]")
                .appName("SparkExample")
                .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
                .config("spark.hadoop.fs.gs.auth.type", "APPLICATION_DEFAULT")
                .config("neo4j.url", NEO4J.getBoltUrl())
                .config("neo4j.authentication.basic.username", "neo4j")
                .config("neo4j.authentication.basic.password", NEO4J.getAdminPassword())
                .getOrCreate();
    }

    @Test
    public void imports_dvd_rental_data_set() throws Exception {
        final String url = NEO4J.getBoltUrl();
        final AuthToken token = AuthTokens.basic("neo4j", NEO4J.getAdminPassword());

        try (var driver = GraphDatabase.driver(url, token)) {
            driver.verifyConnectivity();

            try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
                assertThat(stream).isNotNull();

                try (var reader = new InputStreamReader(stream)) {
                    var importPipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));
                    runSparkImport(driver, importPipeline.executionPlan());
                }
            }

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

    private void runSparkImport(Driver driver, ImportExecutionPlan plan) {
        plan.getGroups().stream()
                .reduce(
                        CompletableFutures.initial(),
                        (previous, currentGroup) -> CompletableFuture.allOf(previous, runGroup(driver, currentGroup)),
                        CompletableFutures::mergeParallel)
                .join();
    }

    private CompletableFuture<Void> runGroup(Driver driver, ImportStepGroup group) {
        var sourceDataFrames = new HashMap<String, Dataset<Row>>();
        try {
            return group.getStages().stream()
                    .reduce(
                            CompletableFutures.initial(),
                            (chain, currentStage) ->
                                    chain.thenCompose((ignored) -> runStage(driver, currentStage, sourceDataFrames)),
                            CompletableFutures::mergeSequential);
        } finally {
            sourceDataFrames.values().forEach(Dataset::unpersist);
        }
    }

    private CompletableFuture<Void> runStage(
            Driver driver, ImportStepStage currentStage, Map<String, Dataset<Row>> sourceDataFrames) {
        return CompletableFuture.runAsync(() -> {
            var stepFutures = new ArrayList<CompletableFuture<Void>>();
            for (var step : currentStage.getSteps()) {
                switch (step) {
                    case SourceStep sourceStep -> indexSource(sourceDataFrames, sourceStep);
                    case NodeTargetStep node -> {
                        var df = sourceDataFrames.get(node.sourceName());
                        stepFutures.add(runNodeImport(node, df));
                    }
                    case RelationshipTargetStep relationship -> {
                        var df = sourceDataFrames.get(relationship.sourceName());
                        stepFutures.add(runRelationshipImport(relationship, df));
                    }
                    case ActionStep action -> stepFutures.add(runAction(driver, action));
                    default -> Assertions.fail("Unexpected step: %s".formatted(step));
                }
            }
            CompletableFuture.allOf(stepFutures.toArray(new CompletableFuture[0]))
                    .join();
        });
    }

    private static void indexSource(Map<String, Dataset<Row>> sourceDataFrames, SourceStep sourceStep) {
        var source = sourceStep.source();
        assertThat(source).isInstanceOf(ParquetSource.class);
        var parquetSource = (ParquetSource) source;
        var df = spark.read().parquet(parquetSource.uri()).cache();
        sourceDataFrames.put(parquetSource.name(), df);
    }

    private CompletableFuture<Void> runNodeImport(NodeTargetStep node, Dataset<Row> df) {
        return CompletableFuture.runAsync(() -> df.select(sourceColumns(node))
                .withColumnsRenamed(columnRenames(node))
                .write()
                .format(NEO4J_DATASOURCE)
                .mode(saveMode(node.writeMode()))
                .option("script", String.join(";\n", schemaStatements(node)))
                .option("labels", labels(node))
                .option("node.keys", keys(node))
                .save());
    }

    private CompletableFuture<Void> runRelationshipImport(RelationshipTargetStep relationship, Dataset<Row> df) {
        var nodeSaveMode = nodeSaveMode(relationship.nodeMatchMode());
        var startNode = relationship.startNode();
        var startNodeKeys = keyProperties(startNode);
        var endNode = relationship.endNode();
        var endNodeKeys = keyProperties(endNode);

        return CompletableFuture.runAsync(() -> df.select(sourceColumns(relationship))
                .write()
                .format(NEO4J_DATASOURCE)
                .mode(saveMode(relationship.writeMode()))
                .option("script", String.join(";\n", schemaStatements(relationship)))
                .option("relationship", relationship.type())
                .option("relationship.save.strategy", "keys")
                .option("relationship.properties", properties(relationship))
                .option("relationship.source.save.mode", nodeSaveMode)
                .option("relationship.source.labels", labels(startNode))
                .option("relationship.source.node.keys", startNodeKeys)
                .option("relationship.target.save.mode", nodeSaveMode)
                .option("relationship.target.labels", labels(endNode))
                .option("relationship.target.node.keys", endNodeKeys)
                .save());
    }

    private static CompletableFuture<Void> runAction(Driver driver, ActionStep action) {
        assertThat(action.action()).isInstanceOf(CypherAction.class);
        return CompletableFuture.runAsync(() -> runAction((CypherAction) action.action(), driver));
    }

    private static Column[] sourceColumns(NodeTargetStep target) {
        return allProperties(target).map(SparkExampleIT::toSourceColumn).toArray(Column[]::new);
    }

    private static Column[] sourceColumns(RelationshipTargetStep target) {
        return Streams.concatAll(
                        allProperties(target),
                        target.startNode().keyProperties().stream(),
                        target.endNode().keyProperties().stream())
                .map(SparkExampleIT::toSourceColumn)
                .toArray(Column[]::new);
    }

    private Map<String, String> columnRenames(NodeTargetStep target) {
        return allProperties(target)
                .collect(Collectors.toMap(PropertyMapping::getSourceField, PropertyMapping::getTargetProperty));
    }

    private String properties(EntityTargetStep target) {
        return allProperties(target)
                .map(mapping -> "%s:%s".formatted(mapping.getSourceField(), mapping.getTargetProperty()))
                .collect(Collectors.joining(","));
    }

    private String keyProperties(EntityTargetStep target) {
        return target.keyProperties().stream()
                .map(mapping -> "%s:%s".formatted(mapping.getSourceField(), mapping.getTargetProperty()))
                .collect(Collectors.joining(","));
    }

    private SaveMode saveMode(WriteMode writeMode) {
        switch (writeMode) {
            case CREATE -> {
                return SaveMode.Append;
            }
            case MERGE -> {
                return SaveMode.Overwrite;
            }
        }
        throw new IllegalStateException("unexpected write mode: %s".formatted(writeMode));
    }

    private String nodeSaveMode(NodeMatchMode nodeMatchMode) {
        switch (nodeMatchMode) {
            case MATCH -> {
                return "Match";
            }
            case MERGE -> {
                return "Overwrite";
            }
        }
        throw new IllegalStateException("unexpected node match mode: %s".formatted(nodeMatchMode));
    }

    private String keys(EntityTargetStep target) {
        return target.keyProperties().stream()
                .map(PropertyMapping::getTargetProperty)
                .collect(Collectors.joining(","));
    }

    private String labels(NodeTargetStep target) {
        return target.labels().stream()
                .map(label -> {
                    Optional<String> result = SchemaNames.sanitize(label);
                    assertThat(result)
                            .overridingErrorMessage("Label '%s' could not be sanitized", label)
                            .isPresent();
                    return result.get();
                })
                .collect(Collectors.joining(":", ":", ""));
    }

    private static Stream<PropertyMapping> allProperties(EntityTargetStep target) {
        return Stream.concat(target.keyProperties().stream(), target.nonKeyProperties().stream());
    }

    private static Column toSourceColumn(PropertyMapping mapping) {
        return new Column(mapping.getSourceField());
    }

    private List<String> schemaStatements(NodeTargetStep step) {
        var schema = step.schema();
        if (schema == null) {
            return Collections.emptyList();
        }
        var statements = new ArrayList<String>();
        statements.addAll(schema.getKeyConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE (%s) IS NODE KEY"
                        .formatted(
                                constraint.getName(),
                                sanitize(constraint.getLabel()),
                                constraint.getProperties().stream()
                                        .map(SparkExampleIT::sanitize)
                                        .map(prop -> "%s.%s".formatted("n", prop))
                                        .collect(Collectors.joining(","))))
                .toList());
        statements.addAll(schema.getUniqueConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE (%s) IS UNIQUE"
                        .formatted(
                                constraint.getName(),
                                sanitize(constraint.getLabel()),
                                constraint.getProperties().stream()
                                        .map(SparkExampleIT::sanitize)
                                        .map(prop -> "%s.%s".formatted("n", prop))
                                        .collect(Collectors.joining(","))))
                .toList());
        Map<String, PropertyType> propertyTypes = step.propertyTypes();
        statements.addAll(schema.getTypeConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE n.%s IS :: %s"
                        .formatted(
                                constraint.getName(),
                                sanitize(constraint.getLabel()),
                                sanitize(constraint.getProperty()),
                                propertyType(propertyTypes.get(constraint.getProperty()))))
                .toList());
        return statements;
    }

    private List<String> schemaStatements(RelationshipTargetStep step) {
        var schema = step.schema();
        if (schema == null) {
            return Collections.emptyList();
        }
        var statements = new ArrayList<String>();
        statements.addAll(schema.getKeyConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE (%s) IS RELATIONSHIP KEY"
                        .formatted(
                                constraint.getName(),
                                sanitize(step.type()),
                                constraint.getProperties().stream()
                                        .map(SparkExampleIT::sanitize)
                                        .map(prop -> "%s.%s".formatted("r", prop))
                                        .collect(Collectors.joining(","))))
                .toList());
        statements.addAll(schema.getUniqueConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE (%s) IS UNIQUE"
                        .formatted(
                                constraint.getName(),
                                sanitize(step.type()),
                                constraint.getProperties().stream()
                                        .map(SparkExampleIT::sanitize)
                                        .map(prop -> "%s.%s".formatted("r", prop))
                                        .collect(Collectors.joining(","))))
                .toList());
        Map<String, PropertyType> propertyTypes = step.propertyTypes();
        statements.addAll(schema.getTypeConstraints().stream()
                .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE r.%s IS :: %s"
                        .formatted(
                                constraint.getName(),
                                sanitize(step.type()),
                                sanitize(constraint.getProperty()),
                                propertyType(propertyTypes.get(constraint.getProperty()))))
                .toList());
        return statements;
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
            default -> throw new IllegalArgumentException(String.format("Unsupported property type: %s", propertyType));
        };
    }

    private static void runAction(CypherAction cypherAction, Driver driver) {
        try (var session = driver.session()) {
            var query = cypherAction.getQuery();
            switch (cypherAction.getExecutionMode()) {
                case TRANSACTION ->
                    session.writeTransaction((tx) -> tx.run(query).consume());
                case AUTOCOMMIT -> session.run(query).consume();
            }
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
            var result =
                    session.run("""
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                            WHERE type = $constraintType AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] \
                            RETURN count(*) = 1 AS result""", Map.of("constraintType", constraintType, "label", label, "property", property));
            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertNodeTypeConstraint(Driver driver, String label, String property, String propertyType) {
        try (Session session = driver.session()) {
            var result = session.run("""
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                            WHERE type = 'NODE_PROPERTY_TYPE' AND entityType = 'NODE' AND labelsOrTypes = [$label] AND properties = [$property] AND propertyType = $propertyType \
                            RETURN count(*) = 1 AS result""", Map.of("label", label, "property", property, "propertyType", propertyType));
            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertRelationshipConstraint(
            Driver driver, String constraintType, String relType, String property) {
        try (Session session = driver.session()) {
            var result =
                    session.run("""
                                    SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties \
                                    WHERE type = $constraintType AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] \
                                    RETURN count(*) = 1 AS result""", Map.of("constraintType", constraintType, "type", relType, "property", property));

            var records = result.list(MapAccessor::asMap);
            assertThat(records).hasSize(1);
            assertThat((boolean) records.getFirst().get("result")).isTrue();
        }
    }

    private static void assertRelationshipTypeConstraint(
            Driver driver, String relType, String property, String propertyType) {
        try (Session session = driver.session()) {
            var result = session.run("""
                            SHOW CONSTRAINTS YIELD type, entityType, labelsOrTypes, properties, propertyType \
                            WHERE type = 'RELATIONSHIP_PROPERTY_TYPE' AND entityType = 'RELATIONSHIP' AND labelsOrTypes = [$type] AND properties = [$property] AND propertyType = $propertyType \
                            RETURN count(*) = 1 AS result""", Map.of("type", relType, "property", property, "propertyType", propertyType));

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

        @Override
        public ParquetSource apply(ObjectNode objectNode) {
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

    private static class Streams {

        @SafeVarargs
        public static <T> Stream<T> concatAll(Stream<T>... streams) {
            return Arrays.stream(streams).reduce(Stream::concat).orElse(Stream.empty());
        }
    }
}
