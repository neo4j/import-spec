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
package org.neo4j.importer.v1.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.schemas.SchemaCoder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Wait;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.e2e.AdminImportIT.ThrowingFunction;
import org.neo4j.importer.v1.sources.NamedJdbcSource;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class BeamIT {

    @Rule
    public final TestPipeline pipeline = TestPipeline.create();

    @ClassRule
    public static Neo4jContainer<?> NEO4J = new Neo4jContainer<>(DockerImageName.parse("neo4j:5-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    @ClassRule
    public static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"))
            .withDatabaseName("northwind")
            .withInitScript("e2e/postgres-dump/northwind.sql");

    private Driver neo4jDriver;

    @Before
    public void prepare() {
        neo4jDriver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        neo4jDriver.verifyConnectivity();
    }

    @After
    public void cleanUp() {
        neo4jDriver.close();
    }

    @Test
    public void imports_data_via_Beam_and_json_spec() throws Exception {
        runBeamImport("json");
    }

    @Test
    public void imports_data_via_Beam_and_yaml_spec() throws Exception {
        runBeamImport("yaml");
    }

    private void runBeamImport(String extension) throws Exception {
        var importSpec =
                read("/e2e/beam-import/spec.%s".formatted(extension), ImportSpecificationDeserializer::deserialize);
        Map<String, PCollection<Row>> dependencies = new HashMap<>();
        Map<Source, PCollection<Row>> sourceTransforms =
                new HashMap<>(importSpec.getSources().size());

        Set<Target> targets = new TreeSet<>(importSpec.getTargets().getAll());
        targets.forEach((target) -> {
            String targetName = target.getName();
            Source source = importSpec.findSourceByName(target.getSource());
            PCollection<Row> sourceStart = sourceTransforms.computeIfAbsent(
                    source,
                    (ignored) -> pipeline.apply(
                            "Read rows from source %s".formatted(source.getName()),
                            SourceIO.readAll(
                                    source, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())));
            PCollection<Row> targetOutput = sourceStart
                    .apply(
                            "Wait on %s dependencies".formatted(targetName),
                            Wait.on(dependenciesOf(target, dependencies)))
                    .setCoder(SchemaCoder.of(sourceStart.getSchema()))
                    .apply(
                            "Perform %s import to Neo4j".formatted(targetName),
                            TargetIO.writeAll(NEO4J.getBoltUrl(), NEO4J.getAdminPassword(), importSpec, target));
            dependencies.put(targetName, targetOutput);
        });

        pipeline.run();

        var productCount = neo4jDriver
                .executableQuery("MATCH (p:Product) RETURN count(p) AS count")
                .execute()
                .records();
        assertThat(productCount).hasSize(1);
        assertThat(productCount.getFirst().get("count").asLong()).isEqualTo(77L);
        var categoryCount = neo4jDriver
                .executableQuery("MATCH (c:Category) RETURN count(c) AS count")
                .execute()
                .records();
        assertThat(categoryCount).hasSize(1);
        assertThat(categoryCount.getFirst().get("count").asLong()).isEqualTo(8L);
        var productInCategoryCount = neo4jDriver
                .executableQuery("MATCH (:Product)-[btc:BELONGS_TO_CATEGORY]->(:Category) RETURN count(btc) AS count")
                .execute()
                .records();
        assertThat(productInCategoryCount).hasSize(1);
        assertThat(productInCategoryCount.getFirst().get("count").asLong()).isEqualTo(77L);
    }

    private List<PCollection<?>> dependenciesOf(Target target, Map<String, PCollection<Row>> processedTargets) {
        return allDependenciesOf(target).stream().map(processedTargets::get).collect(Collectors.toList());
    }

    private List<String> allDependenciesOf(Target target) {
        if (!(target instanceof RelationshipTarget relationshipTarget)) {
            return target.getDependencies();
        }
        List<String> dependencies = new ArrayList<>(target.getDependencies());
        String startReference = relationshipTarget.getStartNodeReference();
        if (startReference != null) {
            dependencies.add(startReference);
        }
        String endReference = relationshipTarget.getEndNodeReference();
        if (endReference != null) {
            dependencies.add(endReference);
        }
        return dependencies;
    }

    private <T> T read(String classpathResource, ThrowingFunction<Reader, T> fn) throws Exception {
        InputStream stream = this.getClass().getResourceAsStream(classpathResource);
        assertThat(stream).isNotNull();
        try (var reader = new InputStreamReader(stream)) {
            return fn.apply(reader);
        }
    }
}

class SourceIO extends PTransform<@NotNull PBegin, @NotNull PCollection<Row>> {

    private final Source source;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private SourceIO(Source source, String jdbcUrl, String username, String password) {
        this.source = source;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public static PTransform<@NotNull PBegin, @NotNull PCollection<Row>> readAll(
            Source source, String jdbcUrl, String username, String password) {
        return new SourceIO(source, jdbcUrl, username, password);
    }

    @Override
    public @NotNull PCollection<Row> expand(@NotNull PBegin input) {
        return input.apply(
                "Fetching rows of JDBC source %s".formatted(source.getName()),
                JdbcIO.readRows()
                        .withDataSourceConfiguration(DataSourceConfiguration.create("org.postgresql.Driver", jdbcUrl)
                                .withUsername(username)
                                .withPassword(password))
                        .withQuery(((NamedJdbcSource) source).getSql()));
    }
}

class TargetIO extends PTransform<@NotNull PCollection<Row>, @NotNull PCollection<Row>> {

    private final String url;
    private final String password;
    private final ImportSpecification importSpec;
    private final Target target;

    private TargetIO(String url, String password, ImportSpecification importSpec, Target target) {
        this.url = url;
        this.password = password;
        this.importSpec = importSpec;
        this.target = target;
    }

    public static PTransform<@NotNull PCollection<Row>, @NotNull PCollection<Row>> writeAll(
            String url, String password, ImportSpecification importSpec, Target target) {
        return new TargetIO(url, password, importSpec, target);
    }

    @Override
    public @NotNull PCollection<Row> expand(@NotNull PCollection<Row> input) {
        return input.apply("Write rows to Neo4j", ParDo.of(TargetWriteRowFn.of(url, password, importSpec, target)))
                .setCoder(SchemaCoder.of(input.getSchema()));
    }
}

class TargetWriteRowFn extends DoFn<Row, Row> {

    private final String url;
    private final String password;
    private final ImportSpecification importSpec;
    private final Target target;
    private transient Driver driver;

    private TargetWriteRowFn(String url, String password, ImportSpecification importSpec, Target target) {
        this.url = url;
        this.password = password;
        this.importSpec = importSpec;
        this.target = target;
    }

    public static DoFn<Row, Row> of(String url, String password, ImportSpecification importSpec, Target target) {
        return new TargetWriteRowFn(url, password, importSpec, target);
    }

    @Setup
    public void setUp() {
        driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
        driver.verifyConnectivity();
    }

    @ProcessElement
    public void processElement(ProcessContext context) {
        Row row = context.element();
        if (target instanceof CustomQueryTarget queryTarget) {
            driver.executableQuery(queryTarget.getQuery()).execute();
        }

        if (target instanceof NodeTarget nodeTarget) {
            driver.executableQuery("%s ".formatted(writeMode(nodeTarget))
                            + "(n:%s%s) "
                                    .formatted(
                                            String.join(":", nodeTarget.getLabels()), nodePattern(nodeTarget, "props"))
                            + "SET n += $props")
                    .withParameters(Map.of("props", propertiesFor(nodeTarget.getProperties(), row)))
                    .execute();
        }

        if (target instanceof RelationshipTarget relationshipTarget) {
            NodeTarget start = importSpec.getTargets().getNodes().stream()
                    .filter(node -> node.getName().equals(relationshipTarget.getStartNodeReference()))
                    .findFirst()
                    .orElseThrow();
            NodeTarget end = importSpec.getTargets().getNodes().stream()
                    .filter(node -> node.getName().equals(relationshipTarget.getEndNodeReference()))
                    .findFirst()
                    .orElseThrow();
            driver.executableQuery(
                            "%1$s (start:%2$s%3$s) %1$s (end:%4$s%5$s) %6$s (start)-[r:%7$s]->(end) SET r = $props"
                                    .formatted(
                                            nodeMatchMode(relationshipTarget),
                                            String.join(":", start.getLabels()),
                                            nodePattern(start, "start"),
                                            String.join(":", end.getLabels()),
                                            nodePattern(end, "end"),
                                            writeMode(relationshipTarget),
                                            relationshipTarget.getType()))
                    .withParameters(Map.of(
                            "start", nodeKeyValues(start, row),
                            "end", nodeKeyValues(end, row),
                            "props", propertiesFor(relationshipTarget.getProperties(), row)))
                    .execute();
        }
        context.output(row);
    }

    private Set<String> nodeKeyPropertyNames(NodeTarget node) {
        Set<String> keyProps = new HashSet<>();

        node.getSchema().getNodeKeyConstraints().forEach(kc -> keyProps.addAll(kc.getProperties()));

        if (keyProps.isEmpty()) {
            node.getProperties().forEach(p -> keyProps.add(p.getTargetProperty()));
        }

        return keyProps;
    }

    private Map<String, Object> nodeKeyValues(NodeTarget node, Row row) {
        return nodeKeyPropertyNames(node).stream()
                .map(name -> node.getProperties().stream()
                        .filter(p -> p.getTargetProperty().equals(name))
                        .findFirst()
                        .orElseThrow())
                .map(mapping -> Map.entry(mapping.getTargetProperty(), propertyValue(mapping, row)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private String nodePattern(NodeTarget nodeTarget, String parameterName) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        List<PropertyMapping> mappings = nodeTarget.getProperties();
        List<NodeKeyConstraint> nodeKeyConstraints = nodeTarget.getSchema().getNodeKeyConstraints();
        for (int i = 0; i < nodeKeyConstraints.size(); i++) {
            NodeKeyConstraint key = nodeKeyConstraints.get(i);
            List<String> properties = key.getProperties();
            for (int j = 0; j < properties.size(); j++) {
                String property = properties.get(j);
                PropertyMapping mapping = mappings.stream()
                        .filter(m -> m.getTargetProperty().equals(property))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "could not find property mapping for property %s".formatted(property)));
                String targetProperty = mapping.getTargetProperty();
                builder.append("`%1$s`:$%2$s.`%1$s`".formatted(targetProperty, parameterName));
                if (i != nodeKeyConstraints.size() - 1 || j != properties.size() - 1) {
                    builder.append(",");
                }
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private Map<String, Object> propertiesFor(List<PropertyMapping> properties, Row row) {
        return properties.stream()
                .map(mapping -> Map.entry(mapping.getTargetProperty(), propertyValue(mapping, row)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private static Object propertyValue(PropertyMapping mapping, Row row) {
        return row.getValue(mapping.getSourceField());
    }

    private static String writeMode(NodeTarget target) {
        return target.getWriteMode().name();
    }

    private static String writeMode(RelationshipTarget target) {
        return target.getWriteMode().name();
    }

    private static String nodeMatchMode(RelationshipTarget target) {
        return target.getNodeMatchMode().name();
    }

    @Teardown
    public void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }
}
