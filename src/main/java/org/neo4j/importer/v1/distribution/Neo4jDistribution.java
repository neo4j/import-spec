package org.neo4j.importer.v1.distribution;

public class Neo4jDistribution {
    private final String versionString;
    private final Neo4jDistributions.Edition edition;
    private final Neo4jDistributions.Version version;

    protected Neo4jDistribution(Neo4jDistributions.Edition edition, Neo4jDistributions.Version version) {
        this.edition = edition;
        this.version = version;
        this.versionString = String.format("Neo4j %s %s", version, edition);
    }

    public boolean isVersionHigherThanLTS() {
        return version.isBiggerThanOrEqual("4.4");
    }

    public boolean isEnterprise() {
        return edition != Neo4jDistributions.Edition.COMMUNITY;
    }

    public boolean hasNodeTypeConstraints() {
        return isEnterprise() && version.isBiggerThanOrEqual("5.9");
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
        return version.isBiggerThanOrEqual("5.13");
    }

    public boolean hasRelationshipKeyConstraints() {
        return isEnterprise() && version.isBiggerThanOrEqual("5.7");
    }

    public boolean hasRelationshipTypeConstraints() {
        return isEnterprise() && version.isBiggerThanOrEqual("5.9");
    }

    public boolean hasRelationshipUniqueConstraints() {
        return version.isBiggerThanOrEqual("5.7");
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
        return version.isBiggerThanOrEqual("5.13");
    }

    @Override
    public String toString() {
        return versionString;
    }
}
