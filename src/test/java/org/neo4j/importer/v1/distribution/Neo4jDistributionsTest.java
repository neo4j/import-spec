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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class Neo4jDistributionsTest {

    @Test
    public void version_of_should_discard_aura_string_in_the_end() {
        var dist = Neo4jDistributions.enterprise().of("5.19-aura");
        assertThat(dist)
                .isEqualTo(new Neo4jDistribution(
                        Neo4jDistributions.Edition.ENTERPRISE, new Neo4jDistributions.Version(5, 19)));
    }

    @Test
    public void version_of_should_parse_version_string_with_patch_version() {
        var dist = Neo4jDistributions.community().of("5.20.0");
        assertThat(dist)
                .isEqualTo(new Neo4jDistribution(
                        Neo4jDistributions.Edition.COMMUNITY, new Neo4jDistributions.Version(5, 20)));
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
