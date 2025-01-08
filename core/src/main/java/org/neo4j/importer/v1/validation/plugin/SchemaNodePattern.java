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
package org.neo4j.importer.v1.validation.plugin;

import java.util.List;
import java.util.Objects;

final class SchemaNodePattern {

    private final String label;
    private final List<String> propertyNames;

    public SchemaNodePattern(String label, String propertyName) {
        this(label, List.of(propertyName));
    }

    public SchemaNodePattern(String label, List<String> propertyNames) {
        this.label = label;
        this.propertyNames = propertyNames;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaNodePattern)) return false;
        SchemaNodePattern that = (SchemaNodePattern) o;
        return Objects.equals(label, that.label) && Objects.equals(propertyNames, that.propertyNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, propertyNames);
    }
}
