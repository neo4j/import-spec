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
package org.neo4j.importer.v1.distribution;

import java.util.Objects;

/**
 * {@link Neo4jDistribution} represents the target distribution of Neo4j, encapsulating its version and edition<br>
 * {@link Neo4jDistribution} class provides methods to check for the availability of various features.
 * <br>
 * Assumptions:
 * - Neo4j 4.4 is considered the minimum supported version. Therefore, certain features (like NodeRangeIndexes,
 * NodePointIndexes etc.) are assumed to be always present
 */
public class Neo4jDistribution {
    private final String versionString;
    private final Neo4jDistributions.Edition edition;
    private final Neo4jDistributions.Version version;

    Neo4jDistribution(Neo4jDistributions.Edition edition, Neo4jDistributions.Version version) {
        this.edition = edition;
        this.version = version;
        this.versionString = String.format("Neo4j %s %s", version, edition);
    }

    /**
     * Whether this database version is larger than or equal to the provided raw version
     * @param versionString raw Neo4j version
     * @return true if this database version is larger than or equal to the provided raw version, false otherwise
     */
    public boolean isVersionLargerThanOrEqual(String versionString) {
        return version.isLargerThanOrEqual(versionString);
    }

    /**
     * Whether this database edition is enterprise
     * @return true if this database edition is enterprise, false otherwise
     */
    public boolean isEnterprise() {
        return edition != Neo4jDistributions.Edition.COMMUNITY;
    }

    /**
     * Whether this database supports node property type constraints
     * @return true if this database supports node property type constraints, false otherwise
     */
    public boolean hasNodeTypeConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.9");
    }

    /**
     * Whether this database supports node key constraints
     * @return true if this database supports node key constraints, false otherwise
     */
    public boolean hasNodeKeyConstraints() {
        return isEnterprise();
    }

    /**
     * Whether this database supports node unique constraints
     * @return true if this database supports node unique constraints, false otherwise
     */
    public boolean hasNodeUniqueConstraints() {
        return true;
    }

    /**
     * Whether this database supports node property existence constraints
     * @return true if this database supports node property existence constraints, false otherwise
     */
    public boolean hasNodeExistenceConstraints() {
        return isEnterprise();
    }

    /**
     * Whether this database supports node range indexes
     * @return true if this database supports node range indexes, false otherwise
     */
    public boolean hasNodeRangeIndexes() {
        return true;
    }

    /**
     * Whether this database supports node text indexes
     * @return true if this database supports node text indexes, false otherwise
     */
    public boolean hasNodeTextIndexes() {
        return true;
    }

    /**
     * Whether this database supports node point indexes
     * @return true if this database supports node point indexes, false otherwise
     */
    public boolean hasNodePointIndexes() {
        return true;
    }

    /**
     * Whether this database supports node full-text indexes
     * @return true if this database supports node full-text indexes, false otherwise
     */
    public boolean hasNodeFullTextIndexes() {
        return true;
    }

    /**
     * Whether this database supports node vector indexes
     * @return true if this database supports node vector indexes, false otherwise
     */
    public boolean hasNodeVectorIndexes() {
        return version.isLargerThanOrEqual("5.13");
    }

    /**
     * Whether this database supports relationship key constraints
     * @return true if this database supports relationship key constraints, false otherwise
     */
    public boolean hasRelationshipKeyConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.7");
    }

    /**
     * Whether this database supports relationship property type constraints
     * @return true if this database supports relationship property type constraints, false otherwise
     */
    public boolean hasRelationshipTypeConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.9");
    }

    /**
     * Whether this database supports relationship unique constraints
     * @return true if this database supports relationship unique constraints, false otherwise
     */
    public boolean hasRelationshipUniqueConstraints() {
        return version.isLargerThanOrEqual("5.7");
    }

    /**
     * Whether this database supports relationship property existence constraints
     * @return true if this database supports relationship property existence constraints, false otherwise
     */
    public boolean hasRelationshipExistenceConstraints() {
        return isEnterprise();
    }

    /**
     * Whether this database supports relationship range indexes
     * @return true if this database supports relationship range indexes, false otherwise
     */
    public boolean hasRelationshipRangeIndexes() {
        return isEnterprise();
    }

    /**
     * Whether this database supports relationship text indexes
     * @return true if this database supports relationship text indexes, false otherwise
     */
    public boolean hasRelationshipTextIndexes() {
        return true;
    }

    /**
     * Whether this database supports relationship point indexes
     * @return true if this database supports relationship point indexes, false otherwise
     */
    public boolean hasRelationshipPointIndexes() {
        return true;
    }

    /**
     * Whether this database supports relationship full-text indexes
     * @return true if this database supports relationship full-text indexes, false otherwise
     */
    public boolean hasRelationshipFullTextIndexes() {
        return true;
    }

    /**
     * Whether this database supports relationship vector indexes
     * @return true if this database supports relationship vector indexes, false otherwise
     */
    public boolean hasRelationshipVectorIndexes() {
        return version.isLargerThanOrEqual("5.13");
    }

    @Override
    public String toString() {
        return versionString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Neo4jDistribution)) return false;
        Neo4jDistribution that = (Neo4jDistribution) o;
        return edition == that.edition && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edition, version);
    }
}
