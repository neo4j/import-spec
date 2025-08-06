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
package org.neo4j.importer.v1.targets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class NodeSchema implements Schema, Serializable {

    static final NodeSchema EMPTY = new NodeSchema(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private final List<NodeTypeConstraint> typeConstraints;
    private final List<NodeKeyConstraint> keyConstraints;
    private final List<NodeUniqueConstraint> uniqueConstraints;
    private final List<NodeExistenceConstraint> existenceConstraints;
    private final List<NodeRangeIndex> rangeIndexes;
    private final List<NodeTextIndex> textIndexes;
    private final List<NodePointIndex> pointIndexes;
    private final List<NodeFullTextIndex> fullTextIndexes;
    private final List<NodeVectorIndex> vectorIndexes;

    @JsonCreator
    public NodeSchema(
            @JsonProperty("type_constraints") List<NodeTypeConstraint> typeConstraints,
            @JsonProperty("key_constraints") List<NodeKeyConstraint> keyConstraints,
            @JsonProperty("unique_constraints") List<NodeUniqueConstraint> uniqueConstraints,
            @JsonProperty("existence_constraints") List<NodeExistenceConstraint> existenceConstraints,
            @JsonProperty("range_indexes") List<NodeRangeIndex> rangeIndexes,
            @JsonProperty("text_indexes") List<NodeTextIndex> textIndexes,
            @JsonProperty("point_indexes") List<NodePointIndex> pointIndexes,
            @JsonProperty("fulltext_indexes") List<NodeFullTextIndex> fullTextIndexes,
            @JsonProperty("vector_indexes") List<NodeVectorIndex> vectorIndexes) {

        this.typeConstraints = typeConstraints;
        this.keyConstraints = keyConstraints;
        this.uniqueConstraints = uniqueConstraints;
        this.existenceConstraints = existenceConstraints;
        this.rangeIndexes = rangeIndexes;
        this.textIndexes = textIndexes;
        this.pointIndexes = pointIndexes;
        this.fullTextIndexes = fullTextIndexes;
        this.vectorIndexes = vectorIndexes;
    }

    public boolean isEmpty() {
        return getTypeConstraints().isEmpty()
                && getKeyConstraints().isEmpty()
                && getUniqueConstraints().isEmpty()
                && getExistenceConstraints().isEmpty()
                && getRangeIndexes().isEmpty()
                && getTextIndexes().isEmpty()
                && getPointIndexes().isEmpty()
                && getFullTextIndexes().isEmpty()
                && getVectorIndexes().isEmpty();
    }

    public List<NodeTypeConstraint> getTypeConstraints() {
        return typeConstraints != null ? typeConstraints : List.of();
    }

    public List<NodeKeyConstraint> getKeyConstraints() {
        return keyConstraints != null ? keyConstraints : List.of();
    }

    public List<NodeUniqueConstraint> getUniqueConstraints() {
        return uniqueConstraints != null ? uniqueConstraints : List.of();
    }

    public List<NodeExistenceConstraint> getExistenceConstraints() {
        return existenceConstraints != null ? existenceConstraints : List.of();
    }

    public List<NodeRangeIndex> getRangeIndexes() {
        return rangeIndexes != null ? rangeIndexes : List.of();
    }

    public List<NodeTextIndex> getTextIndexes() {
        return textIndexes != null ? textIndexes : List.of();
    }

    public List<NodePointIndex> getPointIndexes() {
        return pointIndexes != null ? pointIndexes : List.of();
    }

    public List<NodeFullTextIndex> getFullTextIndexes() {
        return fullTextIndexes != null ? fullTextIndexes : List.of();
    }

    public List<NodeVectorIndex> getVectorIndexes() {
        return vectorIndexes != null ? vectorIndexes : List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeSchema that = (NodeSchema) o;
        return Objects.equals(typeConstraints, that.typeConstraints)
                && Objects.equals(keyConstraints, that.keyConstraints)
                && Objects.equals(uniqueConstraints, that.uniqueConstraints)
                && Objects.equals(existenceConstraints, that.existenceConstraints)
                && Objects.equals(rangeIndexes, that.rangeIndexes)
                && Objects.equals(textIndexes, that.textIndexes)
                && Objects.equals(pointIndexes, that.pointIndexes)
                && Objects.equals(fullTextIndexes, that.fullTextIndexes)
                && Objects.equals(vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                typeConstraints,
                keyConstraints,
                uniqueConstraints,
                existenceConstraints,
                rangeIndexes,
                textIndexes,
                pointIndexes,
                fullTextIndexes,
                vectorIndexes);
    }

    @Override
    public String toString() {
        return "NodeSchema{" + "typeConstraints="
                + typeConstraints + ", keyConstraints="
                + keyConstraints + ", uniqueConstraints="
                + uniqueConstraints + ", existenceConstraints="
                + existenceConstraints + ", rangeIndexes="
                + rangeIndexes + ", textIndexes="
                + textIndexes + ", pointIndexes="
                + pointIndexes + ", fullTextIndexes="
                + fullTextIndexes + ", vectorIndexes="
                + vectorIndexes + '}';
    }
}
