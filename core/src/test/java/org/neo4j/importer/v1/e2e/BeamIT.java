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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.schemas.SchemaCoder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Wait;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.plugin.CypherAction;
import org.neo4j.importer.v1.e2e.AdminImportIT.ThrowingFunction;
import org.neo4j.importer.v1.pipeline.ActionStep;
import org.neo4j.importer.v1.pipeline.CustomQueryTargetStep;
import org.neo4j.importer.v1.pipeline.ImportPipeline;
import org.neo4j.importer.v1.pipeline.ImportStep;
import org.neo4j.importer.v1.pipeline.NodeTargetStep;
import org.neo4j.importer.v1.pipeline.RelationshipTargetStep;
import org.neo4j.importer.v1.pipeline.SourceStep;
import org.neo4j.importer.v1.pipeline.TargetStep;
import org.neo4j.importer.v1.sources.JdbcSource;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.PropertyMapping;
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
        try (Session session = neo4jDriver.session(
                SessionConfig.builder().withDatabase("system").build())) {
            session.run("CREATE OR REPLACE DATABASE neo4j WAIT 60 seconds").consume();
        }
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
        String neo4jUrl = NEO4J.getBoltUrl();
        String neo4jPassword = NEO4J.getAdminPassword();
        var importSpec =
                read("/e2e/beam-import/spec.%s".formatted(extension), ImportSpecificationDeserializer::deserialize);
        var importPipeline = ImportPipeline.of(importSpec);

        Map<String, PCollection<?>> outputs = new HashMap<>();
        Map<String, PCollection<Row>> sourceOutputs =
                new HashMap<>(importSpec.getSources().size());
        importPipeline.forEach(importStep -> {
            switch (importStep) {
                case SourceStep step -> {
                    var name = step.name();
                    PCollection<Row> output = pipeline.apply(
                            "[Source %s] Read rows".formatted(name),
                            SourceIO.readAll(
                                    step.source(),
                                    POSTGRES.getJdbcUrl(),
                                    POSTGRES.getUsername(),
                                    POSTGRES.getPassword()));
                    sourceOutputs.put(name, output);
                    outputs.put(name, output);
                }
                case TargetStep step -> {
                    var name = step.name();
                    PCollection<Row> targetOutput = sourceOutputs
                            .get(step.sourceName())
                            .apply(
                                    "[Target %s] Wait for dependencies".formatted(name),
                                    Wait.on(dependencyOutputs(step.dependencies(), outputs)))
                            .setCoder(SchemaCoder.of(
                                    sourceOutputs.get(step.sourceName()).getSchema()))
                            .apply(
                                    "[Target %s] Perform import to Neo4j".formatted(name),
                                    TargetIO.writeAll(neo4jUrl, neo4jPassword, step));
                    outputs.put(name, targetOutput);
                }
                case ActionStep step -> {
                    var name = step.name();
                    var action = step.action();
                    assertThat(action).isInstanceOf(CypherAction.class);
                    var cypherAction = (CypherAction) action;
                    PCollection<Integer> output = pipeline.apply(
                                    "[Action %s] Create single input".formatted(name), Create.of(1))
                            .apply(
                                    "[Action %s] Wait for dependencies".formatted(name),
                                    Wait.on(dependencyOutputs(step.dependencies(), outputs)))
                            .setCoder(VarIntCoder.of())
                            .apply(
                                    "[action %s] Run".formatted(name),
                                    ParDo.of(CypherActionFn.of(cypherAction, neo4jUrl, neo4jPassword)));
                    outputs.put(name, output);
                }
                default -> Assertions.fail("Unsupported step type: %s".formatted(importStep.getClass()));
            }
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
        var countRows = neo4jDriver.executableQuery("""
                                 MATCH (post_s:Count {stage: 'post_sources'})
                                 MATCH  (pre_n:Count {stage: 'pre_nodes'})
                                 MATCH (post_n:Count {stage: 'post_nodes'})
                                 MATCH  (pre_r:Count {stage: 'pre_relationships'})
                                 MATCH (post_r:Count {stage: 'post_relationships'})
                                 MATCH  (pre_q:Count {stage: 'pre_queries'})
                                 MATCH (post_q:Count {stage: 'post_queries'})
                                 MATCH    (end:Count {stage: 'end'})
                                 RETURN
                                    post_s.count AS post_s_count,
                                    pre_n.count  AS pre_n_count,
                                    post_n.count AS post_n_count,
                                    pre_r.count  AS pre_r_count,
                                    post_r.count AS post_r_count,
                                    pre_q.count  AS pre_q_count,
                                    post_q.count AS post_q_count,
                                    end.count    AS end_count
                                 """.stripIndent()).execute().records();
        assertThat(countRows).hasSize(1);
        Record counts = countRows.getFirst();
        assertThat(counts.get("post_s_count").asLong())
                .isGreaterThanOrEqualTo(0); // targets have likely already started
        assertThat(counts.get("pre_n_count").asLong()).isEqualTo(0);
        assertThat(counts.get("post_n_count").asLong()).isEqualTo(77L + 8L); // 77 (:Product) + 8 (:Category)
        assertThat(counts.get("pre_r_count").asLong()).isEqualTo(0);
        assertThat(counts.get("post_r_count").asLong()).isEqualTo(77L); // 77 -[:BELONGS_TO_CATEGORY]-> rels
        assertThat(counts.get("pre_q_count").asLong()).isEqualTo(0);
        assertThat(counts.get("post_q_count").asLong())
                .isEqualTo(77L + 8L); // 77 (:ClonedProduct) + 8 (:ClonedCategory)
        assertThat(counts.get("end_count").asLong())
                .isEqualTo(77L + 8L + 77L
                        + 8L); // 77 (:Product) + 8 (:Category) + 77 (:ClonedProduct) + 8 (:ClonedCategory)
    }

    private static List<PCollection<?>> dependencyOutputs(
            Set<ImportStep> dependencies, Map<String, PCollection<?>> outputs) {
        return dependencies.stream().map(dep -> outputs.get(dep.name())).collect(Collectors.toUnmodifiableList());
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
                        .withQuery(((JdbcSource) source).getSql()));
    }
}

class TargetIO extends PTransform<@NotNull PCollection<Row>, @NotNull PCollection<Row>> {

    private final String url;
    private final String password;
    private final TargetStep step;

    private TargetIO(String url, String password, TargetStep step) {
        this.url = url;
        this.password = password;
        this.step = step;
    }

    public static PTransform<@NotNull PCollection<Row>, @NotNull PCollection<Row>> writeAll(
            String url, String password, TargetStep step) {
        return new TargetIO(url, password, step);
    }

    @Override
    public @NotNull PCollection<Row> expand(@NotNull PCollection<Row> input) {
        return input.apply("Write rows to Neo4j", ParDo.of(TargetWriteRowFn.of(url, password, step)))
                .setCoder(SchemaCoder.of(input.getSchema()));
    }
}

class CypherActionFn extends DoFn<Integer, Integer> {

    private final AtomicBoolean called = new AtomicBoolean(false);
    private final CypherAction action;
    private final String url;
    private final String password;
    private Driver driver;

    private CypherActionFn(CypherAction action, String url, String password) {
        this.action = action;
        this.url = url;
        this.password = password;
    }

    public static CypherActionFn of(CypherAction action, String url, String password) {
        return new CypherActionFn(action, url, password);
    }

    @Setup
    public void setUp() {
        driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
        driver.verifyConnectivity();
    }

    @ProcessElement
    public void processElement(ProcessContext context) {
        if (!called.compareAndSet(false, true)) {
            return;
        }
        var query = action.getQuery();
        switch (action.getExecutionMode()) {
            case TRANSACTION -> driver.executableQuery(query).execute();
            case AUTOCOMMIT -> {
                try (Session session = driver.session()) {
                    session.run(query).consume();
                }
            }
        }
        context.output(context.element());
    }

    @Teardown
    public void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }
}

class TargetWriteRowFn extends DoFn<Row, Row> {

    private final String url;
    private final String password;
    private final TargetStep step;
    private transient Driver driver;

    private TargetWriteRowFn(String url, String password, TargetStep step) {
        this.url = url;
        this.password = password;
        this.step = step;
    }

    public static DoFn<Row, Row> of(String url, String password, TargetStep step) {
        return new TargetWriteRowFn(url, password, step);
    }

    @Setup
    public void setUp() {
        driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", password));
        driver.verifyConnectivity();
    }

    @ProcessElement
    public void processElement(ProcessContext context) {
        Row row = context.element();
        switch (step) {
            case CustomQueryTargetStep step ->
                driver.executableQuery(step.query())
                        .withParameters(Map.of("rows", List.of(properties(row))))
                        .execute();
            case NodeTargetStep step -> {
                var keys = step.keyProperties();
                var nonKeys = step.nonKeyProperties();
                driver.executableQuery("%s (n:%s%s) %s"
                                .formatted(
                                        step.writeMode(),
                                        String.join(":", step.labels()),
                                        entityPattern("row", keys),
                                        setClause("n", "row", nonKeys)))
                        .withParameters(Map.of("row", rowValues(keys, nonKeys, row)))
                        .execute();
            }
            case RelationshipTargetStep step -> {
                var start = step.startNode();
                var end = step.endNode();
                var keys = step.keyProperties();
                var nonKeys = step.nonKeyProperties();
                driver.executableQuery("%s (start:%s%s) %s (end:%s%s) %s (start)-[r:%s%s]->(end) %s"
                                .formatted(
                                        step.nodeMatchMode(),
                                        String.join(":", start.labels()),
                                        entityPattern("start", start.keyProperties()),
                                        step.nodeMatchMode(),
                                        String.join(":", end.labels()),
                                        entityPattern("end", end.keyProperties()),
                                        step.writeMode(),
                                        step.type(),
                                        entityPattern("row", keys),
                                        setClause("r", "row", nonKeys)))
                        .withParameters(Map.of(
                                "start", nodeKeyValues(start, row),
                                "end", nodeKeyValues(end, row),
                                "row", rowValues(keys, nonKeys, row)))
                        .execute();
            }
            default -> Assertions.fail("unsupported target type: %s", step.getClass());
        }
        context.output(row);
    }

    private String entityPattern(String parameterName, List<PropertyMapping> keyProperties) {
        StringBuilder builder = new StringBuilder();
        builder.append(" {");
        for (int i = 0; i < keyProperties.size(); i++) {
            PropertyMapping mapping = keyProperties.get(i);
            builder.append(
                    "`%s`:$%s.`%s`".formatted(mapping.getTargetProperty(), parameterName, mapping.getSourceField()));
            if (i != keyProperties.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String setClause(String nodeVariableName, String parameterName, List<PropertyMapping> nonKeyProperties) {
        if (nonKeyProperties.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("SET ");
        for (int i = 0; i < nonKeyProperties.size(); i++) {
            PropertyMapping mapping = nonKeyProperties.get(i);
            builder.append("%s.`%s` = $%s.`%s`"
                    .formatted(nodeVariableName, mapping.getTargetProperty(), parameterName, mapping.getSourceField()));
            if (i != nonKeyProperties.size() - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private Map<String, Object> nodeKeyValues(NodeTargetStep node, Row row) {
        return node.keyProperties().stream()
                .collect(Collectors.toMap(PropertyMapping::getSourceField, mapping -> propertyValue(mapping, row)));
    }

    private Map<String, Object> rowValues(
            List<PropertyMapping> keyMappings, List<PropertyMapping> nonKeyMappings, Row row) {
        var result = new HashMap<String, Object>(keyMappings.size() + nonKeyMappings.size());
        keyMappings.forEach(mapping -> {
            result.put(mapping.getSourceField(), propertyValue(mapping, row));
        });
        nonKeyMappings.forEach(mapping -> {
            result.put(mapping.getSourceField(), propertyValue(mapping, row));
        });
        return result;
    }

    private Map<String, Object> properties(Row row) {
        return row.getSchema().getFieldNames().stream()
                .map(name -> Map.entry(name, row.getValue(name)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private static Object propertyValue(PropertyMapping mapping, Row row) {
        return row.getValue(mapping.getSourceField());
    }

    @Teardown
    public void tearDown() {
        if (driver != null) {
            driver.close();
        }
    }
}
