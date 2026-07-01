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

import static org.neo4j.importer.v1.cypher.SpecSupport.CompositeConstraintType.KEY;
import static org.neo4j.importer.v1.cypher.SpecSupport.CompositeConstraintType.UNIQUE;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeExistenceConstraint;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeTypeConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipExistenceConstraint;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.RelationshipTypeConstraint;

class SpecSupport {

    public static Set<SimpleConstraintType> simpleConstraintTypesOf(String propertyName, EntityTarget target) {
        checkInvalidEntityTarget(target);
        checkPropertyName(propertyName, target);
        if (target instanceof NodeTarget) {
            return simpleNodeConstraintTypes(propertyName, (NodeTarget) target);
        }
        return simpleRelationshipConstraintTypes(propertyName, (RelationshipTarget) target);
    }

    public static Set<CompositeConstraintDefinition> compositeConstraintsOf(EntityTarget target) {
        checkInvalidEntityTarget(target);
        if (target instanceof NodeTarget) {
            return compositeNodeConstraints((NodeTarget) target);
        }
        return compositeRelationshipConstraints((RelationshipTarget) target);
    }

    public static List<String> labels(ImportSpecification spec, NodeReference reference) {
        return spec.getTargets().getNodes().stream()
                .filter(target -> target.getName().equals(reference.getName()))
                .findFirst()
                .get()
                .getLabels();
    }

    private static Set<SimpleConstraintType> simpleNodeConstraintTypes(String propertyName, NodeTarget node) {
        var result = new LinkedHashSet<SimpleConstraintType>(4);
        if (simpleKeyProperties(node).contains(propertyName)) {
            result.add(SimpleConstraintType.KEY);
            /*
             Graph type does not allow `(:Label => {prop :: ANY IS KEY})`
             but it allows `(:Label => {prop :: ANY NOT NULL})`
             and `(:Label => {prop :: ANY NOT NULL IS KEY})`
             KEY implies NOT NULL, so NOT_NULL is always added with KEY
            */
            result.add(SimpleConstraintType.NOT_NULL);
        }
        if (simpleUniqueProperties(node).contains(propertyName)) {
            result.add(SimpleConstraintType.UNIQUE);
        }
        if (notNullProperties(node).contains(propertyName)) {
            result.add(SimpleConstraintType.NOT_NULL);
        }
        if (typedProperties(node).contains(propertyName)) {
            result.add(SimpleConstraintType.TYPED);
        }
        return result;
    }

    private static Set<CompositeConstraintDefinition> compositeNodeConstraints(NodeTarget target) {
        var schema = target.getSchema();
        var keyConstraints = schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new CompositeConstraintDefinition(KEY, constraint.getProperties()));
        var uniqueConstraints = schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new CompositeConstraintDefinition(UNIQUE, constraint.getProperties()));
        return Stream.concat(keyConstraints, uniqueConstraints).collect(toLinkedHashSet());
    }

    private static Set<CompositeConstraintDefinition> compositeRelationshipConstraints(RelationshipTarget target) {
        var schema = target.getSchema();
        var keyConstraints = schema.getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new CompositeConstraintDefinition(KEY, constraint.getProperties()));
        var uniqueConstraints = schema.getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() > 1)
                .map(constraint -> new CompositeConstraintDefinition(UNIQUE, constraint.getProperties()));
        return Stream.concat(keyConstraints, uniqueConstraints).collect(toLinkedHashSet());
    }

    private static Set<SimpleConstraintType> simpleRelationshipConstraintTypes(
            String propertyName, RelationshipTarget relationship) {
        var result = new LinkedHashSet<SimpleConstraintType>(4);
        if (simpleKeyProperties(relationship).contains(propertyName)) {
            result.add(SimpleConstraintType.KEY);
        }
        if (simpleUniqueProperties(relationship).contains(propertyName)) {
            result.add(SimpleConstraintType.UNIQUE);
        }
        if (notNullProperties(relationship).contains(propertyName)) {
            result.add(SimpleConstraintType.NOT_NULL);
        }
        if (typedProperties(relationship).contains(propertyName)) {
            result.add(SimpleConstraintType.TYPED);
        }
        return result;
    }

    private static Set<String> properties(EntityTarget node) {
        return node.getProperties().stream()
                .map(PropertyMapping::getTargetProperty)
                .collect(toLinkedHashSet());
    }

    private static Set<String> simpleKeyProperties(NodeTarget node) {
        return node.getSchema().getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().get(0))
                .collect(toLinkedHashSet());
    }

    private static Set<String> simpleUniqueProperties(NodeTarget node) {
        return node.getSchema().getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().get(0))
                .collect(toLinkedHashSet());
    }

    private static Set<String> notNullProperties(NodeTarget node) {
        return node.getSchema().getExistenceConstraints().stream()
                .map(NodeExistenceConstraint::getProperty)
                .collect(toLinkedHashSet());
    }

    private static Set<String> typedProperties(NodeTarget node) {
        return node.getSchema().getTypeConstraints().stream()
                .map(NodeTypeConstraint::getProperty)
                .collect(toLinkedHashSet());
    }

    private static Set<String> simpleKeyProperties(RelationshipTarget node) {
        return node.getSchema().getKeyConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().get(0))
                .collect(toLinkedHashSet());
    }

    private static Set<String> simpleUniqueProperties(RelationshipTarget node) {
        return node.getSchema().getUniqueConstraints().stream()
                .filter(constraint -> constraint.getProperties().size() == 1)
                .map(constraint -> constraint.getProperties().get(0))
                .collect(toLinkedHashSet());
    }

    private static Set<String> notNullProperties(RelationshipTarget node) {
        return node.getSchema().getExistenceConstraints().stream()
                .map(RelationshipExistenceConstraint::getProperty)
                .collect(toLinkedHashSet());
    }

    private static Set<String> typedProperties(RelationshipTarget node) {
        return node.getSchema().getTypeConstraints().stream()
                .map(RelationshipTypeConstraint::getProperty)
                .collect(toLinkedHashSet());
    }

    private static void checkInvalidEntityTarget(EntityTarget target) {
        if (!(target instanceof NodeTarget) && !(target instanceof RelationshipTarget)) {
            throw new IllegalArgumentException(String.format("Unexpected entity target type %s", target.getClass()));
        }
    }

    private static void checkPropertyName(String propertyName, EntityTarget target) {
        if (!properties(target).contains(propertyName)) {
            throw new IllegalArgumentException(
                    String.format("No such property %s in entity target %s", propertyName, target.getName()));
        }
    }

    private static <T> Collector<T, ?, LinkedHashSet<T>> toLinkedHashSet() {
        return Collectors.toCollection(LinkedHashSet::new);
    }

    static class CompositeConstraintDefinition {

        private final CompositeConstraintType type;
        private final List<String> properties;

        private CompositeConstraintDefinition(CompositeConstraintType type, List<String> properties) {
            this.type = type;
            this.properties = properties;
        }

        public CompositeConstraintType getType() {
            return type;
        }

        public List<String> getProperties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CompositeConstraintDefinition)) return false;
            CompositeConstraintDefinition that = (CompositeConstraintDefinition) o;
            return type == that.type && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, properties);
        }

        @Override
        public String toString() {
            return "CompositeConstraintDefinition{" + "type=" + type + ", properties=" + properties + '}';
        }
    }

    enum SimpleConstraintType {
        KEY,
        UNIQUE,
        NOT_NULL,
        TYPED;
    }

    enum CompositeConstraintType {
        KEY,
        UNIQUE
    }
}
