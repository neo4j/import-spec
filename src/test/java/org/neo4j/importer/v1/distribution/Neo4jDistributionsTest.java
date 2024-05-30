package org.neo4j.importer.v1.distribution;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class Neo4jDistributionsTest {

    @Test
    public void neo4jdistributions_of_should_return_distribution() {
        var dist = Neo4jDistributions.AURA.of("4.4");
        assertThat(dist.toString()).isEqualTo("Neo4j 4.4 AURA");
    }

    @Test
    public void neo4jdistributions_of_with_aura_string_should_return_distribution() {
        var dist = Neo4jDistributions.AURA.of("4.4-aura");
        assertThat(dist.toString()).isEqualTo("Neo4j 4.4 AURA");
    }

    @Test
    public void version_of_should_fail_if_format_is_wrong() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("invalid_version"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Invalid version format: invalid_version. Version format should be {major}.{minor}");
    }

    @Test
    public void version_of_should_fail_if_major_is_unparsable() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("invalid_major.1"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Major version number invalid_major is invalid. Must be a number.");
    }

    @Test
    public void version_of_should_fail_if_minor_is_unparsable() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("1.invalid_minor"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Minor version number invalid_minor is invalid. Must be a number.");
    }

    @Test
    public void version_of_should_fail_if_major_is_negative() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("-1.1"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Version string -1.1 can not contain a negative number.");
    }

    @Test
    public void version_of_should_fail_if_minor_is_negative() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("1.-1"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Version string 1.-1 can not contain a negative number.");
    }
}
