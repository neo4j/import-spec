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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Parser;
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
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.actions.CypherAction;
import org.neo4j.importer.v1.actions.CypherExecutionMode;
import org.neo4j.importer.v1.graph.Graph;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
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
                var specification = ImportSpecificationDeserializer.deserialize(reader);
                var sourceOutputs = new HashMap<Source, PCollection<GenericRecord>>(
                        specification.getSources().size());
                var targetOutputs = new HashMap<String, PCollection<WriteCounters>>();
                var sortedTargets = sortTargets(specification.getTargets().getAll());
                sortedTargets.forEach(target -> {
                    var source = specification.findSourceByName(target.getSource());
                    assertThat(source).isInstanceOf(ParquetSource.class);

                    var sourceRecords = sourceOutputs.computeIfAbsent(source, (src) -> {
                        var parquetSource = (ParquetSource) src;
                        return pipeline.apply(
                                "[source %s] Read records".formatted(parquetSource.getName()),
                                ParquetIO.parseGenericRecords(
                                                (SerializableFunction<GenericRecord, GenericRecord>) record -> record)
                                        .withCoder(GenericRecordCoder.create())
                                        .from(parquetSource.uri()));
                    });

                    var targetOutput = sourceRecords
                            .apply(
                                    "[target %s] Wait for implicit dependencies".formatted(target.getName()),
                                    Wait.on(implicitDependenciesOf(target, targetOutputs)))
                            .setCoder(sourceRecords.getCoder())
                            .apply(
                                    "[target %s] Assign keys to records".formatted(target.getName()),
                                    WithKeys.of((SerializableFunction<GenericRecord, Integer>)
                                            input -> ThreadLocalRandom.current()
                                                    .nextInt(
                                                            Runtime.getRuntime().availableProcessors())))
                            .setCoder(KvCoder.of(VarIntCoder.of(), sourceRecords.getCoder()))
                            .apply(
                                    "[target %s] Group records into batches".formatted(target.getName()),
                                    GroupIntoBatches.ofSize(50))
                            .apply(
                                    "[target %s] Write record batches to Neo4j".formatted(target.getName()),
                                    TargetIO.writeAll(
                                            NEO4J.getBoltUrl(), NEO4J.getAdminPassword(), specification, target));

                    targetOutputs.put(target.getName(), targetOutput);
                });

                specification.getActions().forEach(action -> {
                    assertThat(action).isInstanceOf(CypherAction.class);
                    pipeline.apply("[action %s] Create single input".formatted(action.getName()), Create.of(1))
                            .apply(
                                    "[action %s] Wait for dependencies inferred from stage".formatted(action.getName()),
                                    Wait.on(stageDependenciesOf(action, targetOutputs)))
                            .setCoder(VarIntCoder.of())
                            .apply(
                                    "[action %s] Run".formatted(action.getName()),
                                    CypherActionIO.run(
                                            (CypherAction) action, NEO4J.getBoltUrl(), NEO4J.getAdminPassword()));
                });
            }
        }

        pipeline.run().waitUntilFinish();

        try (var driver =
                GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()))) {
            driver.verifyConnectivity();

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

    private static List<Target> sortTargets(List<Target> targets) {
        Map<Target, Set<Target>> dependencyGraph = new HashMap<>();
        targets.forEach(target -> {
            if (target instanceof RelationshipTarget relationshipTarget) {
                dependencyGraph.put(
                        relationshipTarget,
                        Set.of(
                                findTargetByName(targets, relationshipTarget.getStartNodeReference()),
                                findTargetByName(targets, relationshipTarget.getEndNodeReference())));
            } else {
                dependencyGraph.put(target, Set.of());
            }
        });
        return Graph.runTopologicalSort(dependencyGraph);
    }

    private List<PCollection<?>> implicitDependenciesOf(
            Target target, Map<String, PCollection<WriteCounters>> targetOutputs) {
        if (target instanceof RelationshipTarget relationshipTarget) {
            return List.of(
                    targetOutputs.get(relationshipTarget.getStartNodeReference()),
                    targetOutputs.get(relationshipTarget.getEndNodeReference()));
        }
        return List.of();
    }

    private List<@NonNull PCollection<?>> stageDependenciesOf(
            Action action, Map<String, PCollection<WriteCounters>> targetOutputs) {
        assertThat(action.getStage()).isEqualTo(ActionStage.END);
        return new ArrayList<>(targetOutputs.values());
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

    private static class TargetIO
            extends PTransform<
                    @NonNull PCollection<KV<Integer, Iterable<GenericRecord>>>, @NonNull PCollection<WriteCounters>> {

        private final String url;

        private final String password;

        private final ImportSpecification spec;

        private final Target target;

        private TargetIO(String url, String password, ImportSpecification spec, Target target) {
            this.url = url;
            this.password = password;
            this.spec = spec;
            this.target = target;
        }

        public static PTransform<
                        @NonNull PCollection<KV<Integer, Iterable<GenericRecord>>>, @NonNull PCollection<WriteCounters>>
                writeAll(String boltUrl, String adminPassword, ImportSpecification spec, Target target) {
            return new TargetIO(boltUrl, adminPassword, spec, target);
        }

        @Override
        public @NonNull PCollection<WriteCounters> expand(PCollection<KV<Integer, Iterable<GenericRecord>>> input) {
            return input.apply(ParDo.of(TargetWriteFn.of(url, password, spec, target)));
        }

        private static class TargetWriteFn extends DoFn<KV<Integer, Iterable<GenericRecord>>, WriteCounters> {

            private final String url;

            private final String password;

            private final ImportSpecification spec;

            private final Target target;

            private transient Driver driver;

            public TargetWriteFn(String url, String password, ImportSpecification spec, Target target) {
                this.url = url;
                this.password = password;
                this.spec = spec;
                this.target = target;
            }

            public static DoFn<KV<Integer, Iterable<GenericRecord>>, WriteCounters> of(
                    String url, String password, ImportSpecification spec, Target target) {
                return new TargetWriteFn(url, password, spec, target);
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
                            case NodeTarget nodeTarget -> buildNodeImportQuery(nodeTarget, unwindRows, row);
                            case RelationshipTarget relationshipTarget -> {
                                var startNodeTarget = findTargetByName(
                                        spec.getTargets().getNodes(), relationshipTarget.getStartNodeReference());
                                var endNodeTarget = findTargetByName(
                                        spec.getTargets().getNodes(), relationshipTarget.getEndNodeReference());
                                yield buildRelationshipImportQuery(
                                        relationshipTarget, startNodeTarget, endNodeTarget, unwindRows, row);
                            }
                            default -> throw new RuntimeException(
                                    "unsupported target type: %s".formatted(target.getClass()));
                        };

                var summary = WriteCounters.of(driver.executableQuery(statement.getCypher())
                        .withParameters(Map.of(rows.getName(), parameters(records)))
                        .execute()
                        .summary());
                context.output(summary);
            }

            private static Statement buildNodeImportQuery(
                    NodeTarget nodeTarget, OngoingReading unwindRows, SymbolicName row) {
                var node = cypherNode(nodeTarget, row);
                var nonKeyProps = nonKeyPropertiesOf(nodeTarget, node.getRequiredSymbolicName(), row);
                var query =
                        switch (nodeTarget.getWriteMode()) {
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
                    RelationshipTarget relationshipTarget,
                    NodeTarget startNodeTarget,
                    NodeTarget endNodeTarget,
                    OngoingReading unwindRows,
                    SymbolicName row) {
                var startNode = cypherNode(startNodeTarget, row, "start");
                var endNode = cypherNode(endNodeTarget, row, "end");
                var relationship = startNode
                        .relationshipTo(endNode, relationshipTarget.getType())
                        .named("r")
                        .withProperties(keyPropertiesOf(relationshipTarget, row));
                var nonKeyProps = nonKeyPropertiesOf(relationshipTarget, relationship.getRequiredSymbolicName(), row);

                var queryStart =
                        switch (relationshipTarget.getNodeMatchMode()) {
                            case MATCH -> unwindRows.match(startNode).match(endNode);
                            case MERGE -> unwindRows.merge(startNode).merge(endNode);
                        };
                var query =
                        switch (relationshipTarget.getWriteMode()) {
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

            private static Node cypherNode(NodeTarget nodeTarget, SymbolicName row) {
                return cypherNode(nodeTarget, row, "n");
            }

            private static Node cypherNode(NodeTarget nodeTarget, SymbolicName row, String variableName) {
                List<String> labels = nodeTarget.getLabels();
                return Cypher.node(labels.getFirst(), labels.subList(1, labels.size()))
                        .named(variableName)
                        .withProperties(keyPropertiesOf(nodeTarget, row));
            }

            private static MapExpression keyPropertiesOf(EntityTarget nodeTarget, SymbolicName rowVariable) {
                var keyProperties = nodeTarget.getKeyProperties();
                var expressions = new ArrayList<>(keyProperties.size() * 2);
                keyProperties.forEach(property -> {
                    expressions.add(property);
                    expressions.add(Cypher.property(rowVariable, sourceFieldFor(nodeTarget, property)));
                });
                return Cypher.mapOf(expressions.toArray());
            }

            private static Collection<? extends Expression> nonKeyPropertiesOf(
                    EntityTarget target, SymbolicName entityVariable, SymbolicName rowVariable) {

                Set<String> nonKeyProperties = getNonKeyProperties(target);
                List<Expression> expressions = new ArrayList<>(nonKeyProperties.size() * 2);
                nonKeyProperties.forEach(property -> {
                    expressions.add(Cypher.property(entityVariable, property));
                    expressions.add(Cypher.property(rowVariable, sourceFieldFor(target, property)));
                });
                return expressions;
            }

            private static Set<String> getNonKeyProperties(EntityTarget nodeTarget) {
                Set<String> properties =
                        new HashSet<>(nodeTarget.getAllProperties().size());
                properties.addAll(nodeTarget.getAllProperties());
                properties.removeAll(new HashSet<>(nodeTarget.getKeyProperties()));
                return properties;
            }

            private static String sourceFieldFor(EntityTarget target, String property) {
                var sourceField = target.getProperties().stream()
                        .filter(mapping -> mapping.getTargetProperty().equals(property))
                        .map(PropertyMapping::getSourceField)
                        .findFirst();
                assertThat(sourceField).isPresent();
                return sourceField.get();
            }

            private List<Map<String, Object>> parameters(Iterable<GenericRecord> records) {
                return StreamSupport.stream(records.spliterator(), false)
                        .map(record -> {
                            var fields = record.getSchema().getFields();
                            var values = new HashMap<String, Object>(fields.size());
                            for (Field field : fields) {
                                Object value = record.get(field.name());
                                convertRecordValue(value)
                                        .ifPresent(convertedValue -> values.put(field.name(), convertedValue));
                            }
                            return values;
                        })
                        .collect(Collectors.toUnmodifiableList());
            }

            private static Optional<Object> convertRecordValue(Object value) {
                if (value instanceof Utf8 utf8Value) {
                    return Optional.of(utf8Value.toString());
                }
                if (value instanceof Array<?> arrayValue) {
                    var values = new ArrayList<>(arrayValue.size());
                    for (Object element : arrayValue) {
                        convertRecordValue(element).ifPresent(values::add);
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

        private final int propertiesSet;

        public static WriteCounters of(ResultSummary summary) {
            return new WriteCounters(summary);
        }

        public WriteCounters(ResultSummary summary) {
            var counters = summary.counters();
            this.labelsAdded = counters.labelsAdded();
            this.labelsRemoved = counters.labelsRemoved();
            this.nodesCreated = counters.nodesCreated();
            this.nodesDeleted = counters.nodesDeleted();
            this.propertiesSet = counters.propertiesSet();
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
        public int getPropertiesSet() {
            return propertiesSet;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WriteCounters that)) return false;
            return labelsAdded == that.labelsAdded
                    && labelsRemoved == that.labelsRemoved
                    && nodesCreated == that.nodesCreated
                    && nodesDeleted == that.nodesDeleted
                    && propertiesSet == that.propertiesSet;
        }

        @Override
        public int hashCode() {
            return Objects.hash(labelsAdded, labelsRemoved, nodesCreated, nodesDeleted, propertiesSet);
        }

        @Override
        public String toString() {
            return "WriteCounters{" + "labelsAdded="
                    + labelsAdded + ", labelsRemoved="
                    + labelsRemoved + ", nodesCreated="
                    + nodesCreated + ", nodesDeleted="
                    + nodesDeleted + ", propertiesSet="
                    + propertiesSet + '}';
        }
    }

    private static <T extends Target> T findTargetByName(List<T> targets, String name) {
        var results = targets.stream().filter(t -> t.getName().equals(name)).toList();
        assertThat(results).hasSize(1);
        return results.getFirst();
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