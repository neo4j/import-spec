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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class EntityTarget extends Target {

    @SuppressWarnings("unchecked")
    private static final List<EntityTargetExtensionProvider<? extends EntityTargetExtension>> EXTENSION_PROVIDERS =
            ServiceLoader.load(EntityTargetExtensionProvider.class).stream()
                    .map(provider -> (EntityTargetExtensionProvider<? extends EntityTarget>) provider.get())
                    .collect(Collectors.toList());

    private final WriteMode writeMode;
    private final List<EntityTargetExtension> extensions;
    private final List<PropertyMapping> properties;

    public EntityTarget(
            TargetType targetType,
            Boolean active,
            String name,
            String source,
            List<String> dependencies,
            WriteMode writeMode,
            List<EntityTargetExtension> extensions,
            List<PropertyMapping> properties) {
        super(targetType, active, name, source, dependencies);
        this.writeMode = writeMode;
        this.extensions = extensions;
        this.properties = properties;
    }

    public WriteMode getWriteMode() {
        return writeMode;
    }

    public List<PropertyMapping> getProperties() {
        return properties != null ? properties : List.of();
    }

    public List<EntityTargetExtension> getExtensions() {
        return extensions;
    }

    public <T extends EntityTargetExtension> Optional<T> getExtension(Class<T> type) {
        return extensions.stream()
                .filter(ext -> type.isAssignableFrom(ext.getClass()))
                .map(type::cast)
                .findFirst();
    }

    public abstract Schema getSchema();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EntityTarget)) return false;
        if (!super.equals(o)) return false;
        EntityTarget that = (EntityTarget) o;
        return writeMode == that.writeMode
                && Objects.equals(extensions, that.extensions)
                && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), writeMode, extensions, properties);
    }

    @Override
    public String toString() {
        return "EntityTarget{" + "writeMode="
                + writeMode + ", extensions="
                + extensions + ", properties="
                + properties + "} "
                + super.toString();
    }

    protected static List<EntityTargetExtension> mapExtensions(ObjectNode rawData) {
        if (rawData == null) {
            return List.of();
        }
        return EXTENSION_PROVIDERS.stream()
                .map(provider -> provider.apply(rawData.deepCopy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
