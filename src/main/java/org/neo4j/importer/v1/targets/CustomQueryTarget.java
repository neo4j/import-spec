/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class CustomQueryTarget extends Target {

    private final String query;

    public CustomQueryTarget(
            @JsonProperty(value = "active", defaultValue = DEFAULT_ACTIVE) Boolean active,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty("depends_on") String dependsOn,
            @JsonProperty(value = "query", required = true) String query) {
        super(active, name, source, dependsOn);
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
