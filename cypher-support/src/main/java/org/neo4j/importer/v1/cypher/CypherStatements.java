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
package org.neo4j.importer.v1.cypher;

import static org.neo4j.importer.v1.cypher.SpecSupport.compositeConstraintsOf;
import static org.neo4j.importer.v1.cypher.SpecSupport.labels;
import static org.neo4j.importer.v1.cypher.SpecSupport.simpleConstraintTypesOf;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.cypher.SpecSupport.CompositeConstraintDefinition;
import org.neo4j.importer.v1.cypher.SpecSupport.SimpleConstraintType;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;

/**
 * CypherStatements provides support for the generation
 * of various Cypher statements, based on a provided {@link ImportSpecification}
 * instance
 */
public class CypherStatements {

    /**
     * Static class - do not call this
     */
    private CypherStatements() {}

    /**
     * Generates a Graph Type creation statement corresponding to the graph
     * shape described by the provided {@link ImportSpecification} instance
     *
     * @param spec the import specification
     * @return the Graph Type creation Cypher statement
     */
    public static String graphType(ImportSpecification spec) {
        var builder = new StringBuilder("ALTER CURRENT GRAPH TYPE SET {");
        var nodeTargetIndex = new AtomicInteger(1);
        var nodes = spec.getTargets().getNodes();
        var relationships = spec.getTargets().getRelationships();
        for (int i = 0; i < nodes.size(); i++) {
            var nodeTarget = nodes.get(i);
            if (!nodeTarget.isActive()) {
                continue;
            }
            builder.append("\n\t");
            builder.append(nodeExpression(nodeTargetIndex.getAndIncrement(), nodeTarget));
            if (i < nodes.size() - 1 || !relationships.isEmpty()) {
                builder.append(",");
            }
        }
        var relationshipTargetIndex = new AtomicInteger(1);
        for (int i = 0; i < relationships.size(); i++) {
            var target = relationships.get(i);
            if (!target.isActive()) {
                continue;
            }
            builder.append("\n\t");
            builder.append(relationshipExpression(relationshipTargetIndex.getAndIncrement(), target, spec));
            if (i < relationships.size() - 1) {
                builder.append(",");
            }
        }
        return builder.append("\n}").toString();
    }

    private static String nodeExpression(int index, NodeTarget target) {
        var labels = target.getLabels();
        var variable = String.format("n%d", index);
        var primaryLabel = primaryLabelExpression(labels);
        var impliedLabels = impliedLabelExpression(labels);
        var properties = propertiesExpression(target);
        var requireExpressions = requireExpressions(variable, compositeConstraintsOf(target));
        return String.format(
                "(%s%s => %s{%s})%s", variable, primaryLabel, impliedLabels, properties, requireExpressions);
    }

    private static String relationshipExpression(int index, RelationshipTarget target, ImportSpecification spec) {
        var startLabel = labelExpression(labels(spec, target.getStartNodeReference()));
        var variable = String.format("r%d", index);
        var relType = typeExpression(target.getType());
        var properties = propertiesExpression(target);
        var endLabel = labelExpression(labels(spec, target.getEndNodeReference()));
        var requireExpressions = requireExpressions(variable, compositeConstraintsOf(target));
        return String.format(
                "(%s)-[%s%s => {%s}]->(%s)%s", startLabel, variable, relType, properties, endLabel, requireExpressions);
    }

    private static String primaryLabelExpression(List<String> labels) {
        return labelExpression(labels);
    }

    private static String labelExpression(List<String> labels) {
        return String.format(":%s", labels.get(0));
    }

    private static String typeExpression(String type) {
        return String.format(":%s", type);
    }

    private static String impliedLabelExpression(List<String> labels) {
        if (labels.size() <= 1) {
            return "";
        }
        var labelConjunction = String.join("&", labels.subList(1, labels.size()));
        return String.format(":%s ", labelConjunction);
    }

    private static String propertiesExpression(EntityTarget target) {
        return target.getProperties().stream()
                .flatMap(mapping -> {
                    var property = mapping.getTargetProperty();
                    var simpleConstraintTypes = simpleConstraintTypesOf(property, target);
                    if (simpleConstraintTypes.isEmpty()) {
                        return Stream.empty();
                    }
                    return Stream.of(propertyExpression(mapping, simpleConstraintTypes));
                })
                .collect(Collectors.joining(", "));
    }

    private static String propertyExpression(PropertyMapping mapping, Set<SimpleConstraintType> simpleConstraintTypes) {
        return String.format(
                "%s :: %s%s%s%s",
                mapping.getTargetProperty(),
                // TODO: vector type
                simpleConstraintTypes.contains(SimpleConstraintType.TYPED)
                        ? mapping.getTargetPropertyType().getName()
                        : "ANY",
                simpleConstraintTypes.contains(SimpleConstraintType.NOT_NULL) ? " NOT NULL" : "",
                simpleConstraintTypes.contains(SimpleConstraintType.KEY) ? " IS KEY" : "",
                simpleConstraintTypes.contains(SimpleConstraintType.UNIQUE) ? " IS UNIQUE" : "");
    }

    private static String requireExpressions(
            final String variable, Set<CompositeConstraintDefinition> compositeConstraints) {
        var result = new StringBuilder();
        compositeConstraints.forEach(constraint -> {
            var properties = constraint.getProperties().stream()
                    .map(prop -> String.format("%s.%s", variable, prop))
                    .collect(Collectors.joining(", "));
            result.append("\n");
            result.append(String.format("\t\tREQUIRE (%s) IS %s", properties, constraint.getType()));
        });
        return result.toString();
    }
}
