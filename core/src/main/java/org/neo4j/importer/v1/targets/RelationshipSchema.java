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

/**
 * {@link RelationshipSchema} defines the indices and constraints that must be created for the enclosing {@link RelationshipTarget}.<br>
 * All the indices and constraints below are optional:
 * <ul>
 *     <li>property type constraints (this only works where the corresponding {@link PropertyMapping} has defined a
 *     type, see {@link RelationshipSchema#getTypeConstraints()})</li>
 *     <li>relationship key constraints (see {@link RelationshipSchema#getKeyConstraints()} ()})</li>
 *     <li>relationship unique constraints (see {@link RelationshipSchema#getUniqueConstraints()} ()})</li>
 *     <li>property existence constraints (see {@link RelationshipSchema#getExistenceConstraints()})</li>
 *     <li>range indexes (see {@link RelationshipSchema#getRangeIndexes()})</li>
 *     <li>text indexes (see {@link RelationshipSchema#getTextIndexes()})</li>
 *     <li>point indexes (see {@link RelationshipSchema#getPointIndexes()})</li>
 *     <li>full-text indexes (see {@link RelationshipSchema#getFullTextIndexes()})</li>
 *     <li>vector indexes (see {@link RelationshipSchema#getVectorIndexes()})</li>
 * </ul>
 */
public class RelationshipSchema implements Schema, Serializable {

    static final RelationshipSchema EMPTY = new RelationshipSchema(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private final List<RelationshipTypeConstraint> typeConstraints;
    private final List<RelationshipKeyConstraint> keyConstraints;
    private final List<RelationshipUniqueConstraint> uniqueConstraints;
    private final List<RelationshipExistenceConstraint> existenceConstraints;
    private final List<RelationshipRangeIndex> rangeIndexes;
    private final List<RelationshipTextIndex> textIndexes;
    private final List<RelationshipPointIndex> pointIndexes;
    private final List<RelationshipFullTextIndex> fullTextIndexes;
    private final List<RelationshipVectorIndex> vectorIndexes;

    @JsonCreator
    public RelationshipSchema(
            @JsonProperty("type_constraints") List<RelationshipTypeConstraint> relationshipTypeConstraints,
            @JsonProperty("key_constraints") List<RelationshipKeyConstraint> relationshipKeyConstraints,
            @JsonProperty("unique_constraints") List<RelationshipUniqueConstraint> relationshipUniqueConstraints,
            @JsonProperty("existence_constraints")
                    List<RelationshipExistenceConstraint> relationshipExistenceConstraints,
            @JsonProperty("range_indexes") List<RelationshipRangeIndex> rangeIndexes,
            @JsonProperty("text_indexes") List<RelationshipTextIndex> textIndexes,
            @JsonProperty("point_indexes") List<RelationshipPointIndex> pointIndexes,
            @JsonProperty("fulltext_indexes") List<RelationshipFullTextIndex> fullTextIndexes,
            @JsonProperty("vector_indexes") List<RelationshipVectorIndex> vectorIndexes) {

        this.typeConstraints = relationshipTypeConstraints;
        this.keyConstraints = relationshipKeyConstraints;
        this.uniqueConstraints = relationshipUniqueConstraints;
        this.existenceConstraints = relationshipExistenceConstraints;
        this.rangeIndexes = rangeIndexes;
        this.textIndexes = textIndexes;
        this.pointIndexes = pointIndexes;
        this.fullTextIndexes = fullTextIndexes;
        this.vectorIndexes = vectorIndexes;
    }

    /**
     * Whether this {@link RelationshipSchema} define any constraints and indices
     * @return true if no schema element is defined, false otherwise
     */
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

    /**
     * Returns the defined property type constraints for this {@link RelationshipTarget}
     * @return property type constraints, this is never <code>null</code>
     */
    public List<RelationshipTypeConstraint> getTypeConstraints() {
        return typeConstraints != null ? typeConstraints : List.of();
    }

    /**
     * Returns the defined key constraints for this {@link RelationshipTarget}
     * @return key constraints, this is never <code>null</code>
     */
    public List<RelationshipKeyConstraint> getKeyConstraints() {
        return keyConstraints != null ? keyConstraints : List.of();
    }

    /**
     * Returns the defined unique constraints for this {@link RelationshipTarget}
     * @return unique constraints, this is never <code>null</code>
     */
    public List<RelationshipUniqueConstraint> getUniqueConstraints() {
        return uniqueConstraints != null ? uniqueConstraints : List.of();
    }

    /**
     * Returns the defined property existence constraints for this {@link RelationshipTarget}
     * @return property existence constraints, this is never <code>null</code>
     */
    public List<RelationshipExistenceConstraint> getExistenceConstraints() {
        return existenceConstraints != null ? existenceConstraints : List.of();
    }

    /**
     * Returns the defined range indexes for this {@link RelationshipTarget}
     * @return range indexes, this is never <code>null</code>
     */
    public List<RelationshipRangeIndex> getRangeIndexes() {
        return rangeIndexes != null ? rangeIndexes : List.of();
    }

    /**
     * Returns the defined text indexes for this {@link RelationshipTarget}
     * @return text indexes, this is never <code>null</code>
     */
    public List<RelationshipTextIndex> getTextIndexes() {
        return textIndexes != null ? textIndexes : List.of();
    }

    /**
     * Returns the defined point indexes for this {@link RelationshipTarget}
     * @return point indexes, this is never <code>null</code>
     */
    public List<RelationshipPointIndex> getPointIndexes() {
        return pointIndexes != null ? pointIndexes : List.of();
    }

    /**
     * Returns the defined full-text indexes for this {@link RelationshipTarget}
     * @return full-text indexes, this is never <code>null</code>
     */
    public List<RelationshipFullTextIndex> getFullTextIndexes() {
        return fullTextIndexes != null ? fullTextIndexes : List.of();
    }

    /**
     * Returns the defined vector indexes for this {@link RelationshipTarget}
     * @return vector indexes, this is never <code>null</code>
     */
    public List<RelationshipVectorIndex> getVectorIndexes() {
        return vectorIndexes != null ? vectorIndexes : List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipSchema that = (RelationshipSchema) o;
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
        return "RelationshipSchema{" + "typeConstraints="
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
