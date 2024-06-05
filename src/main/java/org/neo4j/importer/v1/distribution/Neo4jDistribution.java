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
 * Represents a specific distribution of Neo4j, encapsulating the version and edition of the database.
 * This class provides methods to check for the availability of various features and constraints
 * based on the edition and version of the Neo4j distribution.
 * <p>
 * Assumptions:
 * - Neo4j 4.4 is considered the minimum supported version. Therefore, certain features (like NodeRangeIndexes, NodePointIndexes etc.)
 * are assumed to be always present in all supported versions.
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

    public boolean isVersionLargerThanOrEqual(String versionString) {
        return version.isLargerThanOrEqual(versionString);
    }

    public boolean isEnterprise() {
        return edition != Neo4jDistributions.Edition.COMMUNITY;
    }

    public boolean hasNodeTypeConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.9");
    }

    public boolean hasNodeKeyConstraints() {
        return isEnterprise();
    }

    public boolean hasNodeUniqueConstraints() {
        return true;
    }

    public boolean hasNodeExistenceConstraints() {
        return isEnterprise();
    }

    public boolean hasNodeRangeIndexes() {
        return true;
    }

    public boolean hasNodeTextIndexes() {
        return true;
    }

    public boolean hasNodePointIndexes() {
        return true;
    }

    public boolean hasNodeFullTextIndexes() {
        return true;
    }

    public boolean hasNodeVectorIndexes() {
        return version.isLargerThanOrEqual("5.13");
    }

    public boolean hasRelationshipKeyConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.7");
    }

    public boolean hasRelationshipTypeConstraints() {
        return isEnterprise() && version.isLargerThanOrEqual("5.9");
    }

    public boolean hasRelationshipUniqueConstraints() {
        return version.isLargerThanOrEqual("5.7");
    }

    public boolean hasRelationshipExistenceConstraints() {
        return isEnterprise();
    }

    public boolean hasRelationshipRangeIndexes() {
        return isEnterprise();
    }

    public boolean hasRelationshipTextIndexes() {
        return true;
    }

    public boolean hasRelationshipPointIndexes() {
        return true;
    }

    public boolean hasRelationshipFullTextIndexes() {
        return true;
    }

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
