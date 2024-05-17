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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
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
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.actions.CypherAction;
import org.neo4j.importer.v1.e2e.AdminImportIT.ThrowingFunction;
import org.neo4j.importer.v1.graph.Maps;
import org.neo4j.importer.v1.sources.JdbcSource;
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
        Map<Source, PCollection<Row>> sourceOutputs =
                new HashMap<>(importSpec.getSources().size());
        Map<String, PCollection<Row>> targetOutputs = new HashMap<>();

        Map<ActionStage, List<PCollection<?>>> preActions = Maps.mapValues(
                actionsByStage(
                        importSpec, ActionStage.PRE_NODES, ActionStage.PRE_RELATIONSHIPS, ActionStage.PRE_QUERIES),
                (actions) -> actions.stream()
                        .map(action -> {
                            assertThat(action).isInstanceOf(CypherAction.class);
                            return applyPreAction(
                                    action,
                                    CypherActionFn.of(
                                            (CypherAction) action, NEO4J.getBoltUrl(), NEO4J.getAdminPassword()));
                        })
                        .collect(Collectors.toList()));

        Set<Target> targets = new TreeSet<>(importSpec.getTargets().getAll());
        targets.forEach((target) -> {
            String targetName = target.getName();
            Source source = importSpec.findSourceByName(target.getSource());
            PCollection<Row> sourceStart = sourceOutputs.computeIfAbsent(
                    source,
                    (ignored) -> pipeline.apply(
                            "Read rows from source %s".formatted(source.getName()),
                            SourceIO.readAll(
                                    source, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())));
            PCollection<Row> targetOutput = sourceStart
                    .apply(
                            "Wait on %s dependencies".formatted(targetName),
                            Wait.on(dependenciesOf(target, targetOutputs, preActions)))
                    .setCoder(SchemaCoder.of(sourceStart.getSchema()))
                    .apply(
                            "Perform %s import to Neo4j".formatted(targetName),
                            TargetIO.writeAll(neo4jUrl, neo4jPassword, importSpec, target));
            targetOutputs.put(targetName, targetOutput);
        });

        actionsByStage(
                        importSpec,
                        ActionStage.POST_SOURCES,
                        ActionStage.POST_NODES,
                        ActionStage.POST_RELATIONSHIPS,
                        ActionStage.POST_QUERIES,
                        ActionStage.END)
                .forEach((stage, actions) -> {
                    List<PCollection<?>> stageDependencies =
                            postActionDependencies(stage, importSpec, sourceOutputs, targetOutputs);
                    actions.forEach(action -> {
                        assertThat(action).isInstanceOf(CypherAction.class);
                        CypherActionFn doFn = CypherActionFn.of((CypherAction) action, neo4jUrl, neo4jPassword);
                        applyPostAction(action, stageDependencies, doFn);
                    });
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
        var countRows = neo4jDriver
                .executableQuery(
                        """
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
                                 """
                                .stripIndent())
                .execute()
                .records();
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

    private static Map<ActionStage, List<Action>> actionsByStage(
            ImportSpecification importSpec, ActionStage... stages) {
        var allStages = EnumSet.copyOf(Arrays.asList(stages));
        return importSpec.getActions().stream()
                .filter(action -> allStages.contains(action.getStage()))
                .collect(Collectors.groupingBy(Action::getStage));
    }

    private PCollection<?> applyPreAction(Action action, CypherActionFn fn) {
        return pipeline.apply("Create synthetic single input for action %s".formatted(action.getName()), Create.of(1))
                .setCoder(VarIntCoder.of())
                .apply("Run action %s".formatted(action.getName()), ParDo.of(fn));
    }

    private void applyPostAction(Action action, List<PCollection<?>> stageDependencies, CypherActionFn fn) {
        pipeline.apply("Create synthetic single input for action %s".formatted(action.getName()), Create.of(1))
                .apply("Wait for dependencies of stage %s".formatted(action.getStage()), Wait.on(stageDependencies))
                .setCoder(VarIntCoder.of())
                .apply("Run action %s".formatted(action.getName()), ParDo.of(fn));
    }

    private List<PCollection<?>> postActionDependencies(
            ActionStage stage,
            ImportSpecification importSpec,
            Map<Source, PCollection<Row>> sourceOutputs,
            Map<String, PCollection<Row>> targetOutputs) {
        var targets = importSpec.getTargets();
        switch (stage) {
            case POST_SOURCES -> {
                return new ArrayList<>(sourceOutputs.values());
            }
            case POST_NODES -> {
                return resolveTargetOutputs(targets.getNodes(), targetOutputs);
            }
            case POST_RELATIONSHIPS -> {
                return resolveTargetOutputs(targets.getRelationships(), targetOutputs);
            }
            case POST_QUERIES -> {
                return resolveTargetOutputs(targets.getCustomQueries(), targetOutputs);
            }
            case END -> {
                return new ArrayList<>(targetOutputs.values());
            }
        }
        Assertions.fail("unexpected stage %s", stage);
        return null;
    }

    private static List<PCollection<?>> resolveTargetOutputs(
            List<? extends Target> targets, Map<String, PCollection<Row>> namedOutputs) {
        return targets.stream().map(Target::getName).map(namedOutputs::get).collect(Collectors.toList());
    }

    private List<PCollection<?>> dependenciesOf(
            Target target,
            Map<String, PCollection<Row>> processedTargets,
            Map<ActionStage, List<PCollection<?>>> preActions) {
        var result = allDependenciesOf(target).stream()
                .map(name -> (PCollection<?>) processedTargets.get(name))
                .collect(Collectors.toCollection((Supplier<ArrayList<PCollection<?>>>) ArrayList::new));
        switch (target) {
            case NodeTarget n -> result.addAll(preActions.get(ActionStage.PRE_NODES));
            case RelationshipTarget r -> result.addAll(preActions.get(ActionStage.PRE_RELATIONSHIPS));
            case CustomQueryTarget c -> result.addAll(preActions.get(ActionStage.PRE_QUERIES));
            default -> Assertions.fail("unexpected target type %s", target.getClass());
        }
        return result;
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
                        .withQuery(((JdbcSource) source).getSql()));
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
        switch (target) {
            case CustomQueryTarget queryTarget -> driver.executableQuery(queryTarget.getQuery())
                    .withParameters(Map.of("rows", List.of(properties(row))))
                    .execute();
            case NodeTarget nodeTarget -> driver.executableQuery("%s ".formatted(writeMode(nodeTarget))
                            + "(n:%s%s) "
                                    .formatted(
                                            String.join(":", nodeTarget.getLabels()), nodePattern(nodeTarget, "props"))
                            + "SET n += $props")
                    .withParameters(Map.of("props", propertiesFor(nodeTarget.getProperties(), row)))
                    .execute();
            case RelationshipTarget relationshipTarget -> {
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
            default -> {
                Assertions.fail("unsupported target type: %s", target.getClass());
            }
        }
        context.output(row);
    }

    private Set<String> nodeKeyPropertyNames(NodeTarget node) {
        Set<String> keyProps = new HashSet<>();

        node.getSchema().getKeyConstraints().forEach(kc -> keyProps.addAll(kc.getProperties()));

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
        List<NodeKeyConstraint> nodeKeyConstraints = nodeTarget.getSchema().getKeyConstraints();
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

    private Map<String, Object> properties(Row row) {
        return row.getSchema().getFieldNames().stream()
                .map(name -> Map.entry(name, row.getValue(name)))
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
