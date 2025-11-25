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
package org.neo4j.importer.v1.pipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.EntityTargetExtension;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;

public abstract sealed class EntityTargetStep extends TargetStep permits NodeTargetStep, RelationshipTargetStep {

    protected EntityTargetStep(Set<ImportStep> dependencies) {
        super(dependencies);
    }

    public Map<String, PropertyType> propertyTypes() {
        return target().getProperties().stream()
                .filter(mapping -> mapping.getTargetPropertyType() != null)
                .collect(Collectors.toMap(PropertyMapping::getTargetProperty, PropertyMapping::getTargetPropertyType));
    }

    public List<EntityTargetExtension> getExtensions() {
        return target().getExtensions();
    }

    public abstract List<PropertyMapping> keyProperties();

    public List<PropertyMapping> nonKeyProperties() {
        var keys = new HashSet<>(keyProperties());
        return target().getProperties().stream()
                .filter(mapping -> !keys.contains(mapping))
                .collect(Collectors.toUnmodifiableList());
    }

    protected abstract EntityTarget target();

    protected static Map<String, PropertyMapping> indexByPropertyName(List<PropertyMapping> mappings) {
        return mappings.stream().collect(Collectors.toMap(PropertyMapping::getTargetProperty, Function.identity()));
    }
}
