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

public class RelationshipSchema implements Serializable {

    private final boolean enableTypeConstraints;
    private final List<RelationshipKeyConstraint> relationshipKeyConstraints;
    private final List<RelationshipUniqueConstraint> relationshipUniqueConstraints;
    private final List<RelationshipExistenceConstraint> relationshipExistenceConstraints;
    private final List<RelationshipRangeIndex> rangeIndexes;
    private final List<RelationshipTextIndex> textIndexes;
    private final List<RelationshipPointIndex> pointIndexes;
    private final List<RelationshipFullTextIndex> fullTextIndexes;
    private final List<RelationshipVectorIndex> vectorIndexes;

    @JsonCreator
    public RelationshipSchema(
            @JsonProperty("enable_type_constraints") boolean enableTypeConstraints,
            @JsonProperty("key_constraints") List<RelationshipKeyConstraint> relationshipKeyConstraints,
            @JsonProperty("unique_constraints") List<RelationshipUniqueConstraint> relationshipUniqueConstraints,
            @JsonProperty("existence_constraints")
                    List<RelationshipExistenceConstraint> relationshipExistenceConstraints,
            @JsonProperty("range_indexes") List<RelationshipRangeIndex> rangeIndexes,
            @JsonProperty("text_indexes") List<RelationshipTextIndex> textIndexes,
            @JsonProperty("point_indexes") List<RelationshipPointIndex> pointIndexes,
            @JsonProperty("fulltext_indexes") List<RelationshipFullTextIndex> fullTextIndexes,
            @JsonProperty("vector_indexes") List<RelationshipVectorIndex> vectorIndexes) {

        this.enableTypeConstraints = enableTypeConstraints;
        this.relationshipKeyConstraints = relationshipKeyConstraints;
        this.relationshipUniqueConstraints = relationshipUniqueConstraints;
        this.relationshipExistenceConstraints = relationshipExistenceConstraints;
        this.rangeIndexes = rangeIndexes;
        this.textIndexes = textIndexes;
        this.pointIndexes = pointIndexes;
        this.fullTextIndexes = fullTextIndexes;
        this.vectorIndexes = vectorIndexes;
    }

    public boolean isEnableTypeConstraints() {
        return enableTypeConstraints;
    }

    public List<RelationshipKeyConstraint> getRelationshipKeyConstraints() {
        return relationshipKeyConstraints != null ? relationshipKeyConstraints : List.of();
    }

    public List<RelationshipUniqueConstraint> getRelationshipUniqueConstraints() {
        return relationshipUniqueConstraints != null ? relationshipUniqueConstraints : List.of();
    }

    public List<RelationshipExistenceConstraint> getRelationshipExistenceConstraints() {
        return relationshipExistenceConstraints != null ? relationshipExistenceConstraints : List.of();
    }

    public List<RelationshipRangeIndex> getRangeIndexes() {
        return rangeIndexes != null ? rangeIndexes : List.of();
    }

    public List<RelationshipTextIndex> getTextIndexes() {
        return textIndexes != null ? textIndexes : List.of();
    }

    public List<RelationshipPointIndex> getPointIndexes() {
        return pointIndexes != null ? pointIndexes : List.of();
    }

    public List<RelationshipFullTextIndex> getFullTextIndexes() {
        return fullTextIndexes != null ? fullTextIndexes : List.of();
    }

    public List<RelationshipVectorIndex> getVectorIndexes() {
        return vectorIndexes != null ? vectorIndexes : List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipSchema that = (RelationshipSchema) o;
        return enableTypeConstraints == that.enableTypeConstraints
                && Objects.equals(relationshipKeyConstraints, that.relationshipKeyConstraints)
                && Objects.equals(relationshipUniqueConstraints, that.relationshipUniqueConstraints)
                && Objects.equals(relationshipExistenceConstraints, that.relationshipExistenceConstraints)
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
                relationshipKeyConstraints,
                relationshipUniqueConstraints,
                relationshipExistenceConstraints,
                rangeIndexes,
                textIndexes,
                pointIndexes,
                fullTextIndexes,
                vectorIndexes);
    }

    @Override
    public String toString() {
        return "RelationshipSchema{" + "enableTypeConstraints="
                + enableTypeConstraints + ", relationshipKeyConstraints="
                + relationshipKeyConstraints + ", relationshipUniqueConstraints="
                + relationshipUniqueConstraints + ", relationshipExistenceConstraints="
                + relationshipExistenceConstraints + ", rangeIndexes="
                + rangeIndexes + ", textIndexes="
                + textIndexes + ", pointIndexes="
                + pointIndexes + ", fullTextIndexes="
                + fullTextIndexes + ", vectorIndexes="
                + vectorIndexes + '}';
    }
}
