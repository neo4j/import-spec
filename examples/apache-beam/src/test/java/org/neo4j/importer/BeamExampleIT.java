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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Parser;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData.Array;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.beam.sdk.coders.AtomicCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.extensions.avro.coders.AvroCoder;
import org.apache.beam.sdk.io.parquet.ParquetIO;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupIntoBatches;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.Wait;
import org.apache.beam.sdk.transforms.WithKeys;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.MapExpression;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReading;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.cypherdsl.core.internal.SchemaNames;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.actions.plugin.CypherExecutionMode;
import org.neo4j.importer.v1.pipeline.ActionStep;
import org.neo4j.importer.v1.pipeline.EntityTargetStep;
import org.neo4j.importer.v1.pipeline.ImportPipeline;
import org.neo4j.importer.v1.pipeline.ImportStep;
import org.neo4j.importer.v1.pipeline.NodeTargetStep;
import org.neo4j.importer.v1.pipeline.RelationshipTargetStep;
import org.neo4j.importer.v1.pipeline.SourceStep;
import org.neo4j.importer.v1.pipeline.TargetStep;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.PropertyType;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public class BeamExampleIT {

    @Rule
    public final TestPipeline pipeline = TestPipeline.create();

    @ClassRule
    public static Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    @Test
    public void imports_dvd_rental_data_set() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();

            try (var reader = new InputStreamReader(stream)) {
                var outputs = new HashMap<String, PCollection<?>>();
                var importPipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));
                importPipeline.forEach(step -> {
                    switch (step) {
                        case SourceStep source -> handleSource(source, outputs);
                        case ActionStep action -> handleAction(action, outputs);
                        case TargetStep target -> handleTarget(target, outputs);
                        default -> throw new IllegalStateException("Unexpected value: " + step);
                    }
                });
            }
        }

        pipeline.run().waitUntilFinish();

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

    private void handleSource(SourceStep step, Map<String, PCollection<?>> outputs) {
        var name = step.name();
        var source = step.source();
        assertThat(source).isInstanceOf(ParquetSource.class);
        var parquetSource = (ParquetSource) source;
        var output = pipeline.apply(
                "[source %s] Read records".formatted(name),
                ParquetIO.parseGenericRecords((SerializableFunction<GenericRecord, GenericRecord>) record -> record)
                        .withCoder(GenericRecordCoder.create())
                        .from(parquetSource.uri()));
        outputs.put(source.getName(), output);
    }

    private void handleAction(ActionStep step, Map<String, PCollection<?>> outputs) {
        var actionName = step.name();
        var action = step.action();
        assertThat(action).isInstanceOf(CypherAction.class);
        var cypherAction = (CypherAction) action;
        PCollection<Integer> output = pipeline.apply(
                        "[action %s] Create single input".formatted(actionName), Create.of(1))
                .apply(
                        "[action %s] Wait for dependencies inferred from stage".formatted(actionName),
                        Wait.on(stepsToOutputs(step.dependencies(), outputs)))
                .setCoder(VarIntCoder.of())
                .apply(
                        "[action %s] Run".formatted(actionName),
                        CypherActionIO.run(cypherAction, NEO4J.getBoltUrl(), NEO4J.getAdminPassword()));
        outputs.put(actionName, output);
    }

    @SuppressWarnings("unchecked")
    private void handleTarget(TargetStep step, Map<String, PCollection<?>> outputs) {
        var stepName = step.name();
        assertThat(step).isInstanceOf(EntityTargetStep.class);
        var entityTargetStep = (EntityTargetStep) step;
        var schemaInitOutput = pipeline.apply("[target %s] Create single input".formatted(stepName), Create.of(1))
                .setCoder(VarIntCoder.of())
                .apply(
                        "[target %s] Init schema".formatted(stepName),
                        TargetSchemaIO.initSchema(NEO4J.getBoltUrl(), NEO4J.getAdminPassword(), entityTargetStep));
        var sourceRecords = (PCollection<GenericRecord>) outputs.get(step.sourceName());
        var sourceCoder = sourceRecords.getCoder();
        var output = sourceRecords
                .apply(
                        "[target %s] Wait for implicit dependencies".formatted(stepName),
                        Wait.on(stepsToOutputs(step.dependencies(), outputs, schemaInitOutput)))
                .setCoder(sourceCoder)
                .apply(
                        "[target %s] Assign keys to records".formatted(stepName),
                        WithKeys.of((SerializableFunction<GenericRecord, Integer>) input -> ThreadLocalRandom.current()
                                .nextInt(Runtime.getRuntime().availableProcessors())))
                .setCoder(KvCoder.of(VarIntCoder.of(), sourceCoder))
                .apply("[target %s] Group records into batches".formatted(stepName), GroupIntoBatches.ofSize(50))
                .apply(
                        "[target %s] Write record batches to Neo4j".formatted(stepName),
                        TargetIO.writeAll(NEO4J.getBoltUrl(), NEO4J.getAdminPassword(), entityTargetStep));
        outputs.put(stepName, output);
    }

    private static List<PCollection<?>> stepsToOutputs(
            List<ImportStep> dependencies, Map<String, PCollection<?>> outputs, PCollection<?>... extras) {
        var result = dependencies.stream()
                .map(step -> outputs.get(step.name()))
                .collect(Collectors.toCollection((Supplier<ArrayList<PCollection<?>>>) ArrayList::new));
        Collections.addAll(result, extras);
        return result;
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

    private static class TargetSchemaIO
            extends PTransform<@NonNull PCollection<Integer>, @NonNull PCollection<WriteCounters>> {

        private final String url;

        private final String password;

        private final EntityTargetStep target;

        private TargetSchemaIO(String url, String password, EntityTargetStep target) {
            this.url = url;
            this.password = password;
            this.target = target;
        }

        public static PTransform<@NonNull PCollection<Integer>, @NonNull PCollection<WriteCounters>> initSchema(
                String url, String password, EntityTargetStep target) {
            return new TargetSchemaIO(url, password, target);
        }

        @Override
        public @NonNull PCollection<WriteCounters> expand(@NonNull PCollection<Integer> input) {
            return input.apply(ParDo.of(TargetSchemaWriteFn.of(url, password, target)));
        }

        private static class TargetSchemaWriteFn extends DoFn<Integer, WriteCounters> {

            private final String url;

            private final String password;

            private final EntityTargetStep target;

            private transient Driver driver;

            public TargetSchemaWriteFn(String url, String password, EntityTargetStep target) {
                this.url = url;
                this.password = password;
                this.target = target;
            }

            public static DoFn<Integer, WriteCounters> of(String url, String password, EntityTargetStep target) {
                return new TargetSchemaWriteFn(url, password, target);
            }

            @Setup
            public void setUp() {
                driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
                driver.verifyConnectivity();
            }

            @Teardown
            public void tearDown() {
                if (driver != null) {
                    driver.close();
                }
            }

            @ProcessElement
            @SuppressWarnings("unused")
            public void processElement(ProcessContext context) {
                var schemaStatements =
                        switch (target) {
                            case NodeTargetStep nodeTarget -> generateNodeSchemaStatements(nodeTarget);
                            case RelationshipTargetStep relationshipTarget -> generateRelationshipSchemaStatements(
                                    relationshipTarget);
                            default -> throw new IllegalStateException("Unexpected value: " + target);
                        };

                if (schemaStatements.isEmpty()) {
                    return;
                }
                try (Session session = driver.session()) {
                    List<ResultSummary> summaries = session.executeWrite(tx -> schemaStatements.stream()
                            .map(statement -> tx.run(statement).consume())
                            .toList());
                    context.output(WriteCounters.ofCombined(summaries));
                }
            }

            private List<String> generateNodeSchemaStatements(NodeTargetStep step) {
                var schema = step.schema();
                if (schema == null) {
                    return Collections.emptyList();
                }
                var statements = new ArrayList<String>();
                statements.addAll(schema.getKeyConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE (%s) IS NODE KEY"
                                .formatted(
                                        generateName(step, "key", constraint.getLabel(), constraint.getProperties()),
                                        sanitize(constraint.getLabel()),
                                        constraint.getProperties().stream()
                                                .map(TargetSchemaWriteFn::sanitize)
                                                .map(prop -> propertyOf("n", prop))
                                                .collect(Collectors.joining(","))))
                        .toList());
                statements.addAll(schema.getUniqueConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE (%s) IS UNIQUE"
                                .formatted(
                                        generateName(step, "unique", constraint.getLabel(), constraint.getProperties()),
                                        sanitize(constraint.getLabel()),
                                        constraint.getProperties().stream()
                                                .map(TargetSchemaWriteFn::sanitize)
                                                .map(prop -> propertyOf("n", prop))
                                                .collect(Collectors.joining(","))))
                        .toList());
                Map<String, PropertyType> propertyTypes = step.propertyTypes();
                statements.addAll(schema.getTypeConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR (n:%s) REQUIRE n.%s IS :: %s"
                                .formatted(
                                        generateName(
                                                step, "type", constraint.getLabel(), List.of(constraint.getProperty())),
                                        sanitize(constraint.getLabel()),
                                        sanitize(constraint.getProperty()),
                                        propertyType(propertyTypes.get(constraint.getProperty()))))
                        .toList());
                return statements;
            }

            private List<String> generateRelationshipSchemaStatements(RelationshipTargetStep step) {
                var schema = step.schema();
                if (schema == null) {
                    return Collections.emptyList();
                }
                var statements = new ArrayList<String>();
                statements.addAll(schema.getKeyConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE (%s) IS RELATIONSHIP KEY"
                                .formatted(
                                        generateName(step, "key", step.type(), constraint.getProperties()),
                                        sanitize(step.type()),
                                        constraint.getProperties().stream()
                                                .map(TargetSchemaWriteFn::sanitize)
                                                .map(prop -> propertyOf("r", prop))
                                                .collect(Collectors.joining(","))))
                        .toList());
                statements.addAll(schema.getUniqueConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE (%s) IS UNIQUE"
                                .formatted(
                                        generateName(step, "unique", step.type(), constraint.getProperties()),
                                        sanitize(step.type()),
                                        constraint.getProperties().stream()
                                                .map(TargetSchemaWriteFn::sanitize)
                                                .map(prop -> propertyOf("r", prop))
                                                .collect(Collectors.joining(","))))
                        .toList());
                Map<String, PropertyType> propertyTypes = step.propertyTypes();
                statements.addAll(schema.getTypeConstraints().stream()
                        .map(constraint -> "CREATE CONSTRAINT %s FOR ()-[r:%s]-() REQUIRE r.%s IS :: %s"
                                .formatted(
                                        generateName(step, "type", step.type(), List.of(constraint.getProperty())),
                                        sanitize(step.type()),
                                        sanitize(constraint.getProperty()),
                                        propertyType(propertyTypes.get(constraint.getProperty()))))
                        .toList());
                return statements;
            }

            private static String generateName(
                    EntityTargetStep target, String type, String label, List<String> properties) {
                return sanitize("%s_%s_%s_%s".formatted(target.name(), type, label, String.join("-", properties)));
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
    }

    private static class TargetIO
            extends PTransform<
                    @NonNull PCollection<KV<Integer, Iterable<GenericRecord>>>, @NonNull PCollection<WriteCounters>> {

        private final String url;

        private final String password;

        private final EntityTargetStep target;

        private TargetIO(String url, String password, EntityTargetStep target) {
            this.url = url;
            this.password = password;
            this.target = target;
        }

        public static PTransform<
                        @NonNull PCollection<KV<Integer, Iterable<GenericRecord>>>, @NonNull PCollection<WriteCounters>>
                writeAll(String boltUrl, String adminPassword, EntityTargetStep target) {
            return new TargetIO(boltUrl, adminPassword, target);
        }

        @Override
        public @NonNull PCollection<WriteCounters> expand(PCollection<KV<Integer, Iterable<GenericRecord>>> input) {
            return input.apply(ParDo.of(TargetWriteFn.of(url, password, target)));
        }

        private static class TargetWriteFn extends DoFn<KV<Integer, Iterable<GenericRecord>>, WriteCounters> {

            private final String url;

            private final String password;

            private final EntityTargetStep target;

            private transient Driver driver;

            public TargetWriteFn(String url, String password, EntityTargetStep target) {
                this.url = url;
                this.password = password;
                this.target = target;
            }

            public static DoFn<KV<Integer, Iterable<GenericRecord>>, WriteCounters> of(
                    String url, String password, EntityTargetStep target) {
                return new TargetWriteFn(url, password, target);
            }

            @Setup
            public void setUp() {
                driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
                driver.verifyConnectivity();
            }

            @Teardown
            public void tearDown() {
                if (driver != null) {
                    driver.close();
                }
            }

            @ProcessElement
            @SuppressWarnings("unused")
            public void processElement(ProcessContext context) {
                var rows = Cypher.parameter("rows");
                var row = Cypher.name("row");
                var unwindRows = Cypher.unwind(rows).as(row);

                var element = context.element();
                assertThat(element).isNotNull();
                Iterable<GenericRecord> records = element.getValue();
                assertThat(records).isNotNull();
                var statement =
                        switch (target) {
                            case NodeTargetStep nodeTarget -> buildNodeImportQuery(nodeTarget, unwindRows, row);
                            case RelationshipTargetStep relationshipTarget -> buildRelationshipImportQuery(
                                    relationshipTarget, unwindRows, row);
                            default -> throw new IllegalStateException("Unexpected value: " + target);
                        };

                var summary = WriteCounters.of(driver.executableQuery(statement.getCypher())
                        .withParameters(Map.of(rows.getName(), parameters(records)))
                        .execute()
                        .summary());
                context.output(summary);
            }

            private static Statement buildNodeImportQuery(
                    NodeTargetStep nodeTarget, OngoingReading unwindRows, SymbolicName row) {
                var node = cypherNode(nodeTarget, row);
                var nonKeyProps = nonKeyPropertiesOf(nodeTarget, node.getRequiredSymbolicName(), row);
                var query =
                        switch (nodeTarget.writeMode()) {
                            case CREATE -> {
                                var create = unwindRows.create(node);
                                if (nonKeyProps.isEmpty()) {
                                    yield create;
                                }
                                yield create.set(nonKeyProps);
                            }
                            case MERGE -> {
                                var merge = unwindRows.merge(node);
                                if (nonKeyProps.isEmpty()) {
                                    yield merge;
                                }
                                yield merge.onCreate().set(nonKeyProps);
                            }
                        };
                return query.build();
            }

            private Statement buildRelationshipImportQuery(
                    RelationshipTargetStep relationshipTarget, OngoingReading unwindRows, SymbolicName row) {
                var startNode = cypherNode(relationshipTarget.startNode(), row, "start");
                var endNode = cypherNode(relationshipTarget.endNode(), row, "end");
                var relationship = startNode
                        .relationshipTo(endNode, relationshipTarget.type())
                        .named("r")
                        .withProperties(keyPropertiesOf(relationshipTarget, row));
                var nonKeyProps = nonKeyPropertiesOf(relationshipTarget, relationship.getRequiredSymbolicName(), row);

                var queryStart =
                        switch (relationshipTarget.nodeMatchMode()) {
                            case MATCH -> unwindRows.match(startNode).match(endNode);
                            case MERGE -> unwindRows.merge(startNode).merge(endNode);
                        };
                var query =
                        switch (relationshipTarget.writeMode()) {
                            case CREATE -> {
                                var create = queryStart.create(relationship);
                                if (nonKeyProps.isEmpty()) {
                                    yield create;
                                }
                                yield create.set(nonKeyProps);
                            }
                            case MERGE -> {
                                var merge = queryStart.merge(relationship);
                                if (nonKeyProps.isEmpty()) {
                                    yield merge;
                                }
                                yield merge.set(nonKeyProps);
                            }
                        };
                return query.build();
            }

            private static Node cypherNode(NodeTargetStep nodeTarget, SymbolicName row) {
                return cypherNode(nodeTarget, row, "n");
            }

            private static Node cypherNode(NodeTargetStep nodeTarget, SymbolicName row, String variableName) {
                List<String> labels = nodeTarget.labels();
                return Cypher.node(labels.getFirst(), labels.subList(1, labels.size()))
                        .named(variableName)
                        .withProperties(keyPropertiesOf(nodeTarget, row));
            }

            private static MapExpression keyPropertiesOf(EntityTargetStep target, SymbolicName rowVariable) {
                return Cypher.mapOf(target.keyProperties().stream()
                        .flatMap(mapping -> Stream.of(
                                mapping.getTargetProperty(), Cypher.property(rowVariable, mapping.getSourceField())))
                        .toArray());
            }

            private static Collection<? extends Expression> nonKeyPropertiesOf(
                    EntityTargetStep target, SymbolicName entityVariable, SymbolicName rowVariable) {

                return target.nonKeyProperties().stream()
                        .flatMap(mapping -> Stream.of(
                                Cypher.property(entityVariable, mapping.getTargetProperty()),
                                Cypher.property(rowVariable, mapping.getSourceField())))
                        .toList();
            }

            private List<Map<String, Object>> parameters(Iterable<GenericRecord> records) {
                return StreamSupport.stream(records.spliterator(), false)
                        .map(record -> {
                            var fields = record.getSchema().getFields();
                            var values = new HashMap<String, Object>(fields.size());
                            for (Field field : fields) {
                                Object value = record.get(field.name());
                                convertRecordValue(field, value)
                                        .ifPresent(convertedValue -> values.put(field.name(), convertedValue));
                            }
                            return values;
                        })
                        .collect(Collectors.toUnmodifiableList());
            }

            private static Optional<Object> convertRecordValue(Field field, Object value) {
                if (field.schema().getTypes().stream()
                        .filter(type -> !type.equals(Schema.create(Type.NULL)))
                        .map(Schema::getLogicalType)
                        .anyMatch(type -> LogicalTypes.date().equals(type))) {
                    return Optional.of(LocalDate.ofEpochDay(((Number) value).longValue()));
                }
                if (value instanceof Utf8 utf8Value) {
                    return Optional.of(utf8Value.toString());
                }
                if (value instanceof Array<?> arrayValue) {
                    var values = new ArrayList<>(arrayValue.size());
                    for (Object element : arrayValue) {
                        convertRecordValue(field, element).ifPresent(values::add);
                    }
                    return Optional.of(values);
                }
                if (value instanceof Record) {
                    return Optional.empty(); // Neo4j does not support map properties
                }
                return Optional.ofNullable(value);
            }
        }
    }

    private static class GenericRecordCoder extends AtomicCoder<GenericRecord> {

        private static final ConcurrentHashMap<String, AvroCoder<GenericRecord>> avroCoders = new ConcurrentHashMap<>();

        public static Coder<GenericRecord> create() {
            return new GenericRecordCoder();
        }

        @Override
        public void encode(GenericRecord value, @NonNull OutputStream outStream) throws IOException {
            assertThat(value).isNotNull();
            var schema = value.getSchema();
            String schemaString = schema.toString();
            String schemaHash = getHash(schemaString);
            StringUtf8Coder.of().encode(schemaString, outStream);
            StringUtf8Coder.of().encode(schemaHash, outStream);
            AvroCoder<GenericRecord> coder = avroCoders.computeIfAbsent(schemaHash, s -> AvroCoder.of(schema));
            coder.encode(value, outStream);
        }

        @Override
        public GenericRecord decode(@NonNull InputStream inStream) throws IOException {
            String schemaString = StringUtf8Coder.of().decode(inStream);
            String schemaHash = StringUtf8Coder.of().decode(inStream);
            AvroCoder<GenericRecord> coder =
                    avroCoders.computeIfAbsent(schemaHash, s -> AvroCoder.of(new Parser().parse(schemaString)));
            return coder.decode(inStream);
        }

        private static String getHash(String string) throws CoderException {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new CoderException(e);
            }
            messageDigest.update(string.getBytes());
            return new String(messageDigest.digest());
        }
    }

    private static class WriteCounters implements Serializable {

        private final int labelsAdded;

        private final int labelsRemoved;

        private final int nodesCreated;

        private final int nodesDeleted;

        private final int relationshipsCreated;

        private final int relationshipsDeleted;

        private final int propertiesSet;

        private final int constraintsAdded;

        private final int constraintsRemoved;

        private final int indexesAdded;

        private final int indexesRemoved;

        public static WriteCounters of(ResultSummary summary) {
            return new WriteCounters(summary);
        }

        public WriteCounters(ResultSummary summary) {
            this(asMap(summary.counters()));
        }

        private WriteCounters(Map<String, Integer> counters) {
            this.labelsAdded = counters.getOrDefault("labelsAdded", 0);
            this.labelsRemoved = counters.getOrDefault("labelsRemoved", 0);
            this.nodesCreated = counters.getOrDefault("nodesCreated", 0);
            this.nodesDeleted = counters.getOrDefault("nodesDeleted", 0);
            this.relationshipsCreated = counters.getOrDefault("relationshipsCreated", 0);
            this.relationshipsDeleted = counters.getOrDefault("relationshipsDeleted", 0);
            this.propertiesSet = counters.getOrDefault("propertiesSet", 0);
            this.constraintsAdded = counters.getOrDefault("constraintsAdded", 0);
            this.constraintsRemoved = counters.getOrDefault("constraintsRemoved", 0);
            this.indexesAdded = counters.getOrDefault("indexesAdded", 0);
            this.indexesRemoved = counters.getOrDefault("indexesRemoved", 0);
        }

        public static WriteCounters ofCombined(List<ResultSummary> summaries) {
            var combinedCounters = summaries.stream()
                    .map(summary -> asMap(summary.counters()))
                    .reduce(new HashMap<>(), WriteCounters::combine);
            return new WriteCounters(combinedCounters);
        }

        private static Map<String, Integer> asMap(SummaryCounters counts) {
            Map<String, Integer> result = new HashMap<>(10);
            result.put("labelsAdded", counts.labelsAdded());
            result.put("labelsRemoved", counts.labelsRemoved());
            result.put("nodesCreated", counts.nodesCreated());
            result.put("nodesDeleted", counts.nodesDeleted());
            result.put("relationshipsCreated", counts.relationshipsCreated());
            result.put("relationshipsDeleted", counts.relationshipsDeleted());
            result.put("propertiesSet", counts.propertiesSet());
            result.put("constraintsAdded", counts.constraintsAdded());
            result.put("constraintsRemoved", counts.constraintsRemoved());
            result.put("indexesAdded", counts.indexesAdded());
            result.put("indexesRemoved", counts.indexesRemoved());
            return result;
        }

        private static Map<String, Integer> combine(Map<String, Integer> map1, Map<String, Integer> map2) {
            Map<String, Integer> result = new HashMap<>(10);
            result.put("labelsAdded", map1.getOrDefault("labelsAdded", 0) + map2.getOrDefault("labelsAdded", 0));
            result.put("labelsRemoved", map1.getOrDefault("labelsRemoved", 0) + map2.getOrDefault("labelsRemoved", 0));
            result.put("nodesCreated", map1.getOrDefault("nodesCreated", 0) + map2.getOrDefault("nodesCreated", 0));
            result.put("nodesDeleted", map1.getOrDefault("nodesDeleted", 0) + map2.getOrDefault("nodesDeleted", 0));
            result.put(
                    "relationshipsCreated",
                    map1.getOrDefault("relationshipsCreated", 0) + map2.getOrDefault("relationshipsCreated", 0));
            result.put(
                    "relationshipsDeleted",
                    map1.getOrDefault("relationshipsDeleted", 0) + map2.getOrDefault("relationshipsDeleted", 0));
            result.put("propertiesSet", map1.getOrDefault("propertiesSet", 0) + map2.getOrDefault("propertiesSet", 0));
            result.put(
                    "constraintsAdded",
                    map1.getOrDefault("constraintsAdded", 0) + map2.getOrDefault("constraintsAdded", 0));
            result.put(
                    "constraintsRemoved",
                    map1.getOrDefault("constraintsRemoved", 0) + map2.getOrDefault("constraintsRemoved", 0));
            result.put("indexesAdded", map1.getOrDefault("indexesAdded", 0) + map2.getOrDefault("indexesAdded", 0));
            result.put(
                    "indexesRemoved", map1.getOrDefault("indexesRemoved", 0) + map2.getOrDefault("indexesRemoved", 0));
            return result;
        }

        @SuppressWarnings("unused")
        public int getLabelsAdded() {
            return labelsAdded;
        }

        @SuppressWarnings("unused")
        public int getLabelsRemoved() {
            return labelsRemoved;
        }

        @SuppressWarnings("unused")
        public int getNodesCreated() {
            return nodesCreated;
        }

        @SuppressWarnings("unused")
        public int getNodesDeleted() {
            return nodesDeleted;
        }

        @SuppressWarnings("unused")
        public int getRelationshipsCreated() {
            return relationshipsCreated;
        }

        @SuppressWarnings("unused")
        public int getRelationshipsDeleted() {
            return relationshipsDeleted;
        }

        @SuppressWarnings("unused")
        public int getPropertiesSet() {
            return propertiesSet;
        }

        @SuppressWarnings("unused")
        public int getConstraintsAdded() {
            return constraintsAdded;
        }

        @SuppressWarnings("unused")
        public int getConstraintsRemoved() {
            return constraintsRemoved;
        }

        @SuppressWarnings("unused")
        public int getIndexesAdded() {
            return indexesAdded;
        }

        @SuppressWarnings("unused")
        public int getIndexesRemoved() {
            return indexesRemoved;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WriteCounters that)) return false;
            return labelsAdded == that.labelsAdded
                    && labelsRemoved == that.labelsRemoved
                    && nodesCreated == that.nodesCreated
                    && nodesDeleted == that.nodesDeleted
                    && relationshipsCreated == that.relationshipsCreated
                    && relationshipsDeleted == that.relationshipsDeleted
                    && propertiesSet == that.propertiesSet
                    && constraintsAdded == that.constraintsAdded
                    && constraintsRemoved == that.constraintsRemoved
                    && indexesAdded == that.indexesAdded
                    && indexesRemoved == that.indexesRemoved;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    labelsAdded,
                    labelsRemoved,
                    nodesCreated,
                    nodesDeleted,
                    relationshipsCreated,
                    relationshipsDeleted,
                    propertiesSet,
                    constraintsAdded,
                    constraintsRemoved,
                    indexesAdded,
                    indexesRemoved);
        }

        @Override
        public String toString() {
            return "WriteCounters{" + "labelsAdded="
                    + labelsAdded + ", labelsRemoved="
                    + labelsRemoved + ", nodesCreated="
                    + nodesCreated + ", nodesDeleted="
                    + nodesDeleted + ", relationshipsCreated="
                    + relationshipsCreated + ", relationshipsDeleted="
                    + relationshipsDeleted + ", propertiesSet="
                    + propertiesSet + ", constraintsAdded="
                    + constraintsAdded + ", constraintsRemoved="
                    + constraintsRemoved + ", indexesAdded="
                    + indexesAdded + ", indexesRemoved="
                    + indexesRemoved + '}';
        }
    }

    private static class CypherActionIO
            extends PTransform<@NonNull PCollection<Integer>, @NonNull PCollection<Integer>> {

        private final CypherAction action;

        private final String url;

        private final String password;

        private CypherActionIO(CypherAction action, String url, String password) {
            this.action = action;
            this.url = url;
            this.password = password;
        }

        public static PTransform<@NonNull PCollection<Integer>, @NonNull PCollection<Integer>> run(
                CypherAction action, String url, String password) {
            return new CypherActionIO(action, url, password);
        }

        @Override
        public @NonNull PCollection<Integer> expand(@NonNull PCollection<Integer> input) {
            return input.apply(ParDo.of(new CypherActionFn(action, url, password)));
        }
    }

    private static class CypherActionFn extends DoFn<Integer, Integer> {

        private final CypherAction action;

        private final String url;

        private final String password;

        private transient Driver driver;

        public CypherActionFn(CypherAction action, String url, String password) {
            this.action = action;
            this.url = url;
            this.password = password;
        }

        @Setup
        public void setUp() {
            driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
            driver.verifyConnectivity();
        }

        @Teardown
        public void tearDown() {
            if (driver != null) {
                driver.close();
            }
        }

        @ProcessElement
        public void processElement(ProcessContext context) {
            assertThat(action.getExecutionMode()).isEqualTo(CypherExecutionMode.AUTOCOMMIT);
            try (var session = driver.session()) {
                session.run(action.getQuery()).consume();
            }
            context.output(context.element());
        }
    }
}
