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
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class NodeSchema implements Serializable {

    private final boolean enableTypeConstraints;
    private final List<NodeKeyConstraint> nodeKeyConstraints;
    private final List<NodeUniqueConstraint> nodeUniqueConstraints;
    private final List<NodeExistenceConstraint> nodeExistenceConstraints;
    private final List<NodeRangeIndex> rangeIndexes;
    private final List<NodeTextIndex> textIndexes;
    private final List<NodePointIndex> pointIndexes;
    private final List<NodeFullTextIndex> fullTextIndexes;
    private final List<NodeVectorIndex> vectorIndexes;

    @JsonCreator
    public NodeSchema(
            @JsonProperty("enable_type_constraints") boolean enableTypeConstraints,
            @JsonProperty("key_constraints") List<NodeKeyConstraint> nodeKeyConstraints,
            @JsonProperty("unique_constraints") List<NodeUniqueConstraint> nodeUniqueConstraints,
            @JsonProperty("existence_constraints") List<NodeExistenceConstraint> nodeExistenceConstraints,
            @JsonProperty("range_indexes") List<NodeRangeIndex> rangeIndexes,
            @JsonProperty("text_indexes") List<NodeTextIndex> textIndexes,
            @JsonProperty("point_indexes") List<NodePointIndex> pointIndexes,
            @JsonProperty("fulltext_indexes") List<NodeFullTextIndex> fullTextIndexes,
            @JsonProperty("vector_indexes") List<NodeVectorIndex> vectorIndexes) {

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

    public List<NodeKeyConstraint> getNodeKeyConstraints() {
        return nodeKeyConstraints;
    }

    public List<NodeUniqueConstraint> getNodeUniqueConstraints() {
        return nodeUniqueConstraints;
    }

    public List<NodeExistenceConstraint> getNodeExistenceConstraints() {
        return nodeExistenceConstraints;
    }

    public List<NodeRangeIndex> getRangeIndexes() {
        return rangeIndexes;
    }

    public List<NodeTextIndex> getTextIndexes() {
        return textIndexes;
    }

    public List<NodePointIndex> getPointIndexes() {
        return pointIndexes;
    }

    public List<NodeFullTextIndex> getFullTextIndexes() {
        return fullTextIndexes;
    }

    public List<NodeVectorIndex> getVectorIndexes() {
        return vectorIndexes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeSchema that = (NodeSchema) o;
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
        return "NodeSchema{" + "enableTypeConstraints="
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
