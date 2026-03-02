package org.neo4j.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.NodeExistenceConstraint;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeTypeConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.RelationshipExistenceConstraint;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.RelationshipTypeConstraint;
import org.neo4j.importer.v1.targets.Target;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@Disabled
@Testcontainers
public class GraphTypesIT {

    /**
     * - docker pull 535893049302.dkr.ecr.eu-west-1.amazonaws.com/build-service/neo4j:2026.02.0-enterprise-debian-nightly
     * - docker tag 535893049302.dkr.ecr.eu-west-1.amazonaws.com/build-service/neo4j:2026.02.0-enterprise-debian-nightly neo4j-nightly/neo4j:2026.02-enterprise
     */
    @Container
    public static Neo4jContainer NEO4J = new Neo4jContainer(
                    DockerImageName.parse("neo4j-nightly/neo4j:2026.02-enterprise").asCompatibleSubstituteFor("neo4j"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    private static Driver driver;

    @BeforeAll
    static void prepare() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        driver.verifyConnectivity();
    }

    @AfterAll
    static void cleanUp() {
        driver.close();
    }

    @Test
    void generates_graph_type() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();

            try (var reader = new InputStreamReader(stream)) {
                var spec = ImportSpecificationDeserializer.deserialize(reader);
                var graphType = createGraphType(spec);

                var cypher = graphType.toCypher();
                driver.executableQuery(cypher).execute();

                var actualGraphType = driver.executableQuery(
                                "SHOW CURRENT GRAPH TYPE YIELD specification RETURN specification")
                        .execute()
                        .records()
                        .getFirst()
                        .get("specification")
                        .asString()
                        .trim();
                assertThat(actualGraphType)
                        .contains(" (:`Actor` => {`first_name` :: STRING, `id` :: INTEGER, `last_name` :: STRING}),\n"
                                + " (:`Category` => {`id` :: INTEGER, `name` :: STRING}),\n"
                                + " (:`Customer` => {`creation_date` :: DATE, `email` :: STRING, `first_name` :: STRING, `id` :: INTEGER, `last_name` :: STRING}),\n"
                                + " (:`Inventory` => {`id` :: INTEGER, `movie_id` :: INTEGER}),\n"
                                + " (:`Movie` => {`description` :: STRING, `id` :: INTEGER, `title` :: STRING}),\n"
                                + " (:`Actor` =>)-[:`ACTED_IN` =>]->(:`Movie` =>),\n"
                                + " (:`Customer` =>)-[:`HAS_RENTED` => {`id` :: INTEGER}]->(:`Inventory` =>),\n"
                                + " (:`Movie` =>)-[:`IN_CATEGORY` =>]->(:`Category` =>),");
            }
        }
    }

    private static GraphType createGraphType(ImportSpecification spec) {
        var allNodes = spec.getTargets().getNodes();
        var nodes = allNodes.stream().collect(Collectors.toMap(Target::getName, Function.identity()));
        return new GraphType(
                allNodes.stream()
                        .filter(Target::isActive)
                        .flatMap(target -> toNodeElementType(target).stream())
                        .toList(),
                spec.getTargets().getRelationships().stream()
                        .filter(Target::isActive)
                        .flatMap(target -> toRelationshipElementType(
                                target,
                                nodes.get(target.getStartNodeReference().getName()),
                                nodes.get(target.getEndNodeReference().getName()))
                                .stream())
                        .toList());
    }

    private static Optional<NodeElementType> toNodeElementType(NodeTarget node) {
        var schema = node.getSchema();
        var keyNames = schema.getKeyConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .collect(Collectors.toSet());
        var singleKeyNames = schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().getFirst())
                .collect(Collectors.toSet());
        var uniqueNames = schema.getUniqueConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .collect(Collectors.toSet());
        var singleUniqueNames = schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().getFirst())
                .collect(Collectors.toSet());
        var nonNullNames = schema.getExistenceConstraints().stream()
                .map(NodeExistenceConstraint::getProperty)
                .collect(Collectors.toSet());
        var typedProperties = schema.getTypeConstraints().stream()
                .map(NodeTypeConstraint::getProperty)
                .collect(Collectors.toSet());
        var properties = node.getProperties().stream()
                .filter(mapping -> isCanonicalProperty(mapping, keyNames, uniqueNames, nonNullNames, typedProperties))
                .map(mapping -> {
                    var property = mapping.getTargetProperty();
                    return new Property(
                            property,
                            typeOf(mapping.getTargetPropertyType()),
                            singleKeyNames.contains(property),
                            singleUniqueNames.contains(property),
                            nonNullNames.contains(property));
                })
                .toList();
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        var canonicalProperties = properties.stream().map(p -> p.name).collect(Collectors.toSet());
        return Optional.of(new NodeElementType(
                node.getIdentifyingLabel(),
                node.getImpliedLabels(),
                properties,
                otherConstraints(canonicalProperties, node)));
    }

    private static Optional<RelationshipElementType> toRelationshipElementType(
            RelationshipTarget relationship, NodeTarget startNode, NodeTarget endNode) {
        var schema = relationship.getSchema();
        var keyNames = schema.getKeyConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .collect(Collectors.toSet());
        var singleKeyNames = schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().getFirst())
                .collect(Collectors.toSet());
        var uniqueNames = schema.getUniqueConstraints().stream()
                .flatMap(constraint -> constraint.getProperties().stream())
                .collect(Collectors.toSet());
        var singleUniqueNames = schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().getFirst())
                .collect(Collectors.toSet());
        var nonNullNames = schema.getExistenceConstraints().stream()
                .map(RelationshipExistenceConstraint::getProperty)
                .collect(Collectors.toSet());
        var typedProperties = schema.getTypeConstraints().stream()
                .map(RelationshipTypeConstraint::getProperty)
                .collect(Collectors.toSet());

        var properties = relationship.getProperties().stream()
                .filter(mapping -> isCanonicalProperty(mapping, keyNames, uniqueNames, nonNullNames, typedProperties))
                .map(mapping -> {
                    var property = mapping.getTargetProperty();
                    return new Property(
                            property,
                            typeOf(mapping.getTargetPropertyType()),
                            singleKeyNames.contains(property),
                            singleUniqueNames.contains(property),
                            nonNullNames.contains(property));
                })
                .toList();
        var canonicalProperties = properties.stream().map(p -> p.name).collect(Collectors.toSet());
        return Optional.of(new RelationshipElementType(
                startNode.getIdentifyingLabel(),
                endNode.getIdentifyingLabel(),
                relationship.getType(),
                properties,
                otherConstraints(canonicalProperties, relationship)));
    }

    private static List<Constraint> otherConstraints(Set<String> canonicalProperties, NodeTarget node) {
        // 1 - find non-composite constraints of non-canonical properties
        // TODO: double-check it's actually allowed
        var result = new ArrayList<Constraint>();
        var schema = node.getSchema();
        node.getProperties().stream()
                .filter(mapping -> !canonicalProperties.contains(mapping.getTargetProperty()))
                .flatMap(mapping -> {
                    var property = mapping.getTargetProperty();
                    var constraints = new ArrayList<Constraint>();
                    schema.getKeyConstraints().stream()
                            .filter(constraint -> constraint.getProperties().size() == 1)
                            .filter(constraint ->
                                    constraint.getProperties().getFirst().equals(property))
                            .findFirst()
                            .ifPresent(constraint -> constraints.add(new Constraint(
                                    EntityType.NODE,
                                    node.getIdentifyingLabel(),
                                    constraint.getName(),
                                    ConstraintType.KEY,
                                    List.of(property))));
                    schema.getUniqueConstraints().stream()
                            .filter(constraint -> constraint.getProperties().size() == 1)
                            .filter(constraint ->
                                    constraint.getProperties().getFirst().equals(property))
                            .findFirst()
                            .ifPresent(constraint -> constraints.add(new Constraint(
                                    EntityType.NODE,
                                    node.getIdentifyingLabel(),
                                    constraint.getName(),
                                    ConstraintType.UNIQUE,
                                    List.of(property))));
                    return constraints.stream();
                })
                .forEach(result::add);

        // 2 - find all composite key/unique constraints
        schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new Constraint(
                        EntityType.NODE,
                        node.getIdentifyingLabel(),
                        constraint.getName(),
                        ConstraintType.KEY,
                        constraint.getProperties()))
                .forEach(result::add);
        schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new Constraint(
                        EntityType.NODE,
                        node.getIdentifyingLabel(),
                        constraint.getName(),
                        ConstraintType.UNIQUE,
                        constraint.getProperties()))
                .forEach(result::add);

        return result;
    }

    private static List<Constraint> otherConstraints(Set<String> canonicalProperties, RelationshipTarget relationship) {
        // 1 - find non-composite constraints of non-canonical properties
        // TODO: double-check it's actually allowed
        var result = new ArrayList<Constraint>();
        var schema = relationship.getSchema();
        relationship.getProperties().stream()
                .filter(mapping -> !canonicalProperties.contains(mapping.getTargetProperty()))
                .flatMap(mapping -> {
                    var property = mapping.getTargetProperty();
                    var constraints = new ArrayList<Constraint>();
                    schema.getKeyConstraints().stream()
                            .filter(constraint -> constraint.getProperties().size() == 1)
                            .filter(constraint ->
                                    constraint.getProperties().getFirst().equals(property))
                            .findFirst()
                            .ifPresent(constraint -> constraints.add(new Constraint(
                                    EntityType.RELATIONSHIP,
                                    relationship.getType(),
                                    constraint.getName(),
                                    ConstraintType.KEY,
                                    List.of(property))));
                    schema.getUniqueConstraints().stream()
                            .filter(constraint -> constraint.getProperties().size() == 1)
                            .filter(constraint ->
                                    constraint.getProperties().getFirst().equals(property))
                            .findFirst()
                            .ifPresent(constraint -> constraints.add(new Constraint(
                                    EntityType.RELATIONSHIP,
                                    relationship.getType(),
                                    constraint.getName(),
                                    ConstraintType.UNIQUE,
                                    List.of(property))));
                    return constraints.stream();
                })
                .forEach(result::add);

        // 2 - find all composite key/unique constraints
        schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new Constraint(
                        EntityType.RELATIONSHIP,
                        relationship.getType(),
                        constraint.getName(),
                        ConstraintType.KEY,
                        constraint.getProperties()))
                .forEach(result::add);
        schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new Constraint(
                        EntityType.RELATIONSHIP,
                        relationship.getType(),
                        constraint.getName(),
                        ConstraintType.UNIQUE,
                        constraint.getProperties()))
                .forEach(result::add);

        return result;
    }

    private static boolean isCanonicalProperty(
            PropertyMapping mapping,
            Set<String> singleKeyNames,
            Set<String> uniqueNames,
            Set<String> nonNullNames,
            Set<String> typedProperties) {
        var property = mapping.getTargetProperty();
        return singleKeyNames.contains(property)
                || uniqueNames.contains(property)
                || nonNullNames.contains(property)
                || typedProperties.contains(property);
    }

    private static String typeOf(PropertyType type) {
        if (type == null) {
            return "ANY";
        }
        return switch (type) {
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
            case BYTE_ARRAY -> "ANY";
        };
    }

    record GraphType(List<NodeElementType> nodes, List<RelationshipElementType> relationships) {
        String toCypher() {
            var nodeLabels =
                    nodes.stream().map(NodeElementType::identifyingLabel).toList();
            var relationshipTypes =
                    relationships.stream().map(RelationshipElementType::type).toList();
            var nodeElementTypes = nodes.stream()
                    .map(nodeElementType -> {
                        var index = nodeLabels.indexOf(nodeElementType.identifyingLabel);
                        return nodeElementType.toCypher(index);
                    })
                    .collect(Collectors.joining(",\n    "));
            var relationshipElementTypes = relationships.stream()
                    .map(relationshipElementType -> {
                        var index = relationshipTypes.indexOf(relationshipElementType.type);
                        return relationshipElementType.toCypher(index);
                    })
                    .collect(Collectors.joining(",\n    "));
            var otherConstraints = new ArrayList<String>();
            nodes.stream()
                    .flatMap(nodeElementType -> {
                        var index = nodeLabels.indexOf(nodeElementType.identifyingLabel);
                        return nodeElementType.constraints.stream().map(constraint -> constraint.toCypher(index));
                    })
                    .forEach(otherConstraints::add);
            relationships.stream()
                    .flatMap(relationshipElementType -> {
                        var index = relationshipTypes.indexOf(relationshipElementType.type);
                        return relationshipElementType.constraints.stream()
                                .map(constraint -> constraint.toCypher(index));
                    })
                    .forEach(otherConstraints::add);
            if (!nodeElementTypes.isEmpty() && (!relationshipElementTypes.isEmpty() || !otherConstraints.isEmpty())) {
                nodeElementTypes += ",\n    ";
            }
            if (!relationshipElementTypes.isEmpty() && !otherConstraints.isEmpty()) {
                relationshipElementTypes += ",\n    ";
            }
            return """
                    ALTER CURRENT GRAPH TYPE SET {
                        %s%s%s
                    }
                    """.stripLeading()
                    .formatted(nodeElementTypes, relationshipElementTypes, String.join(",\n    ", otherConstraints));
        }
    }

    record NodeElementType(
            String identifyingLabel,
            List<String> impliedLabels,
            List<Property> properties,
            List<Constraint> constraints) {

        public String toCypher(int index) {
            return "(n%d:%s => %s{%s})"
                    .formatted(
                            index,
                            identifyingLabel,
                            impliedLabels.isEmpty()
                                    ? ""
                                    : impliedLabels.stream().collect(Collectors.joining("&", ":", " ")),
                            properties.stream().map(Property::toCypher).collect(Collectors.joining(", ")));
        }
    }

    record RelationshipElementType(
            String startIdentifyingLabel,
            String endIdentifyingLabel,
            String type,
            List<Property> properties,
            List<Constraint> constraints) {
        public String toCypher(int index) {
            return "(:%s)-[r%d:%s => {%s}]->(:%s)"
                    .formatted(
                            startIdentifyingLabel,
                            index,
                            type,
                            properties.stream().map(Property::toCypher).collect(Collectors.joining(", ")),
                            endIdentifyingLabel);
        }
    }

    record Property(String name, String type, boolean singleKey, boolean singleUnique, boolean nonNull) {
        public String toCypher() {
            return "%s :: %s%s%s%s"
                    .formatted(
                            name,
                            type,
                            singleKey ? " IS KEY" : "",
                            singleUnique ? " IS UNIQUE" : "",
                            nonNull ? " NON NULL" : "");
        }
    }

    record Constraint(
            EntityType entity, String labelOrType, String name, ConstraintType type, List<String> properties) {

        public String toCypher(int index) {
            return switch (entity) {
                case NODE ->
                    "CONSTRAINT %s FOR (n%d:%s) REQUIRE (%s) IS %s"
                            .formatted(
                                    name,
                                    index,
                                    labelOrType,
                                    properties.stream()
                                            .map(p -> "n%d.%s".formatted(index, p))
                                            .collect(Collectors.joining(", ")),
                                    type.name());
                case RELATIONSHIP ->
                    "CONSTRAINT %s FOR ()-[r%d:%s]-() REQUIRE (%s) IS %s"
                            .formatted(
                                    name,
                                    index,
                                    labelOrType,
                                    properties.stream()
                                            .map(p -> "r%d.%s".formatted(index, p))
                                            .collect(Collectors.joining(", ")),
                                    type.name());
            };
        }
    }

    enum EntityType {
        NODE,
        RELATIONSHIP
    }

    enum ConstraintType {
        KEY,
        UNIQUE,
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
}
