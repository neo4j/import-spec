/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
import java.util.List;
import java.util.Objects;

public class RelationshipSchema {

    private final boolean enableTypeConstraints;
    private final List<RelationshipKeyConstraint> nodeKeyConstraints;
    private final List<RelationshipUniqueConstraint> nodeUniqueConstraints;
    private final List<RelationshipExistenceConstraint> nodeExistenceConstraints;
    private final List<RelationshipRangeIndex> rangeIndexes;
    private final List<RelationshipTextIndex> textIndexes;
    private final List<RelationshipPointIndex> pointIndexes;
    private final List<RelationshipFullTextIndex> fullTextIndexes;
    private final List<RelationshipVectorIndex> vectorIndexes;

    @JsonCreator
    public RelationshipSchema(
            @JsonProperty("enable_type_constraints") boolean enableTypeConstraints,
            @JsonProperty("key_constraints") List<RelationshipKeyConstraint> nodeKeyConstraints,
            @JsonProperty("unique_constraints") List<RelationshipUniqueConstraint> nodeUniqueConstraints,
            @JsonProperty("existence_constraints") List<RelationshipExistenceConstraint> nodeExistenceConstraints,
            @JsonProperty("range_indexes") List<RelationshipRangeIndex> rangeIndexes,
            @JsonProperty("text_indexes") List<RelationshipTextIndex> textIndexes,
            @JsonProperty("point_indexes") List<RelationshipPointIndex> pointIndexes,
            @JsonProperty("fulltext_indexes") List<RelationshipFullTextIndex> fullTextIndexes,
            @JsonProperty("vector_indexes") List<RelationshipVectorIndex> vectorIndexes) {

        this.enableTypeConstraints = enableTypeConstraints;
        this.nodeKeyConstraints = nodeKeyConstraints;
        this.nodeUniqueConstraints = nodeUniqueConstraints;
        this.nodeExistenceConstraints = nodeExistenceConstraints;
        this.rangeIndexes = rangeIndexes;
        this.textIndexes = textIndexes;
        this.pointIndexes = pointIndexes;
        this.fullTextIndexes = fullTextIndexes;
        this.vectorIndexes = vectorIndexes;
    }

    public boolean isEnableTypeConstraints() {
        return enableTypeConstraints;
    }

    public List<RelationshipKeyConstraint> getNodeKeyConstraints() {
        return nodeKeyConstraints;
    }

    public List<RelationshipUniqueConstraint> getNodeUniqueConstraints() {
        return nodeUniqueConstraints;
    }

    public List<RelationshipExistenceConstraint> getNodeExistenceConstraints() {
        return nodeExistenceConstraints;
    }

    public List<RelationshipRangeIndex> getRangeIndexes() {
        return rangeIndexes;
    }

    public List<RelationshipTextIndex> getTextIndexes() {
        return textIndexes;
    }

    public List<RelationshipPointIndex> getPointIndexes() {
        return pointIndexes;
    }

    public List<RelationshipFullTextIndex> getFullTextIndexes() {
        return fullTextIndexes;
    }

    public List<RelationshipVectorIndex> getVectorIndexes() {
        return vectorIndexes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipSchema that = (RelationshipSchema) o;
        return enableTypeConstraints == that.enableTypeConstraints
                && Objects.equals(nodeKeyConstraints, that.nodeKeyConstraints)
                && Objects.equals(nodeUniqueConstraints, that.nodeUniqueConstraints)
                && Objects.equals(nodeExistenceConstraints, that.nodeExistenceConstraints)
                && Objects.equals(rangeIndexes, that.rangeIndexes)
                && Objects.equals(textIndexes, that.textIndexes)
                && Objects.equals(pointIndexes, that.pointIndexes)
                && Objects.equals(fullTextIndexes, that.fullTextIndexes)
                && Objects.equals(vectorIndexes, that.vectorIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enableTypeConstraints,
                nodeKeyConstraints,
                nodeUniqueConstraints,
                nodeExistenceConstraints,
                rangeIndexes,
                textIndexes,
                pointIndexes,
                fullTextIndexes,
                vectorIndexes);
    }

    @Override
    public String toString() {
        return "RelationshipSchema{" + "enableTypeConstraints="
                + enableTypeConstraints + ", nodeKeyConstraints="
                + nodeKeyConstraints + ", nodeUniqueConstraints="
                + nodeUniqueConstraints + ", nodeExistenceConstraints="
                + nodeExistenceConstraints + ", rangeIndexes="
                + rangeIndexes + ", textIndexes="
                + textIndexes + ", pointIndexes="
                + pointIndexes + ", fullTextIndexes="
                + fullTextIndexes + ", vectorIndexes="
                + vectorIndexes + '}';
    }
}
