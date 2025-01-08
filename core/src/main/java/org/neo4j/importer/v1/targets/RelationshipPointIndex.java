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
import java.util.Map;
import java.util.Objects;

public class RelationshipPointIndex extends Index {
    private final String property;
    private final Map<String, Object> options;

    @JsonCreator
    public RelationshipPointIndex(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "property", required = true) String property,
            @JsonProperty("options") Map<String, Object> options) {

        super(name);
        this.property = property;
        this.options = options;
    }

    public String getProperty() {
        return property;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RelationshipPointIndex that = (RelationshipPointIndex) o;
        return Objects.equals(property, that.property) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), property, options);
    }

    @Override
    public String toString() {
        return "RelationshipPointIndex{" + "property='"
                + property + '\'' + ", options="
                + options + "} "
                + super.toString();
    }
}
