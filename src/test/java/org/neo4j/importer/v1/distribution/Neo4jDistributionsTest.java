package org.neo4j.importer.v1.distribution;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class Neo4jDistributionsTest {

    @Test
    public void version_of_should_discard_aura_string_in_the_end() {
        var dist = Neo4jDistributions.enterprise().of("5.19-aura");
        assertThat(dist.toString()).isEqualTo("Neo4j 5.19 ENTERPRISE");
    }

    @Test
    public void version_of_should_parse_version_string_with_patch_version() {
        var dist = Neo4jDistributions.community().of("5.20.0");
        assertThat(dist.toString()).isEqualTo("Neo4j 5.20 COMMUNITY");
    }

    @Test
    public void version_of_should_fail_if_minor_version_is_absent() {
        assertThatThrownBy(() -> Neo4jDistributions.Version.of("5"))
                .isInstanceOf(InvalidNeo4jVersionException.class)
                .hasMessage("Minor version should be specified. Examples: 5.9, 5.17");
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
