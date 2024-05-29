package org.neo4j.importer.v1;

import java.util.Locale;
import java.util.Objects;

//todo: check version feature matrix
public class Neo4jDistribution {
    private final String versionString;
    private final Neo4jVersion version;
    private final Neo4jEdition edition;

    public Neo4jDistribution(String version, String edition) {
        this.version = Neo4jVersion.of(version);
        this.edition = Neo4jEdition.of(version, edition);
        this.versionString = String.format("Neo4j %s %s", version, edition);
    }

    public boolean hasConstraints() {
        return edition != Neo4jEdition.COMMUNITY;
    }

    public boolean hasNodeTypeConstraints() {
        return hasConstraints() && version.isBiggerThanOrEqual("5.9");
    }

    public boolean hasNodeKeyConstraints() {
        return hasConstraints();
    }

    public boolean hasNodeUniqueConstraints() {
        return true;
    }

    public boolean hasNodeExistenceConstraints() {
        return hasConstraints();
    }

    public boolean hasNodeRangeIndexes() {
        return hasConstraints();
    }

    public boolean hasNodeTextIndexes() {
        return hasConstraints();
    }

    public boolean hasNodePointIndexes() {
        return hasConstraints();
    }

    public boolean hasNodeFullTextIndexes() {
        return hasConstraints();
    }

    public boolean hasNodeVectorIndexes() {
        return hasConstraints();
    }

    public boolean hasRelationshipKeyConstraints() {
        return hasConstraints() && version.isBiggerThanOrEqual("5.7");
    }

    public boolean hasRelationshipTypeConstraints() {
        return hasConstraints() && version.isBiggerThanOrEqual("5.9");
    }

    public boolean hasRelationshipUniqueConstraints() {
        return version.isBiggerThanOrEqual("5.7");
    }

    public boolean hasRelationshipExistenceConstraints() {
        return hasRelationshipKeyConstraints();
    }

    public boolean hasRelationshipRangeIndexes() {
        return hasRelationshipKeyConstraints();
    }

    public boolean hasRelationshipTextIndexes() {
        return hasRelationshipKeyConstraints();
    }

    public boolean hasRelationshipPointIndexes() {
        return hasRelationshipKeyConstraints();
    }

    public boolean hasRelationshipFullTextIndexes() {
        return hasRelationshipKeyConstraints();
    }

    public boolean hasRelationshipVectorIndexes() {
        return hasRelationshipKeyConstraints();
    }

    @Override
    public String toString() {
        return versionString;
    }

    enum Neo4jEdition {
        COMMUNITY,
        ENTERPRISE,
        AURA;

        public static Neo4jEdition of(String version, String edition) {
            if (version.toLowerCase(Locale.ROOT).endsWith("-aura")) {
                return AURA;
            }

            return Neo4jEdition.valueOf(edition.toUpperCase(Locale.ROOT));
        }
    }

    static class Neo4jVersion {
        private final int major;
        private final int minor;

        private Neo4jVersion(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public static Neo4jVersion of(String version) {
            String[] parts = version.split("\\."); // todo: take care of funky inputs
            return new Neo4jVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        public boolean isBiggerThanOrEqual(String version) {
            var givenVersion = of(version);
            return this.major > givenVersion.major
                    || (this.major == givenVersion.major && (this.minor >= givenVersion.minor));
        }

        @Override
        public String toString() {
            return String.format("%s.%s", major, minor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Neo4jVersion)) return false;
            Neo4jVersion that = (Neo4jVersion) o;
            return Objects.equals(major, that.major) && Objects.equals(minor, that.minor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor);
        }
    }
}
