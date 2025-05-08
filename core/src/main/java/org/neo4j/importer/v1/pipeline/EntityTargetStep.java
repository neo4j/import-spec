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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.EntityTargetExtension;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;

public abstract class EntityTargetStep extends TargetStep {

    protected EntityTargetStep(List<ImportStep> dependencies) {
        super(dependencies);
    }

    public Map<String, PropertyType> propertyTypes() {
        return target().getProperties().stream()
                .filter(mapping -> mapping.getTargetPropertyType() != null)
                .collect(Collectors.toMap(PropertyMapping::getTargetProperty, PropertyMapping::getTargetPropertyType));
    }

    public List<EntityTargetExtension> extensions() {
        return target().getExtensions();
    }

    public abstract List<PropertyMapping> keyProperties();

    public abstract List<PropertyMapping> nonKeyProperties();

    protected abstract EntityTarget target();
}
