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
import java.util.List;
import java.util.Objects;

/**
 * {@link CustomQueryTarget} allows users to arbitrary map data from a particular source to the target database, with
 * the means of a Cypher query.<br>
 * In particular, {@link CustomQueryTarget} defines these mandatory attributes:
 * <ul>
 *     <li>the name of the source is maps data from ({@link CustomQueryTarget#getSource()})</li>
 *     <li>the Cypher query that performs the import</li>
 * </ul>
 * {@link CustomQueryTarget} can optionally specify:<br>
 * <ul>
 *     <li>whether the target is active or not ({@link CustomQueryTarget#isActive()}): backends must skip inactive targets</li>
 *     <li>dependencies on other targets (by their names, see {@link CustomQueryTarget#getDependencies()})</li>
 * </ul>
 * Note: depending on the backend, the specified query may be re-run several times (backends may batch source data,
 * hence running the query at least once per batch, or more if the backend retries).
 */
public class CustomQueryTarget extends Target {

    private final String query;

    @JsonCreator
    public CustomQueryTarget(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty("depends_on") List<String> dependencies,
            @JsonProperty(value = "query", required = true) String query) {
        super(TargetType.QUERY, active, name, source, dependencies);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CustomQueryTarget that = (CustomQueryTarget) o;
        return Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), query);
    }

    @Override
    public String toString() {
        return "CustomQueryTarget{" + "query='" + query + '\'' + "} " + super.toString();
    }
}
