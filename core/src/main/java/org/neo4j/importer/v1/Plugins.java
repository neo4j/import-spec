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
package org.neo4j.importer.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class Plugins {

    static List<SpecificationValidator> loadSpecificationValidators(ImportSpecificationSettings settings) {
        var providers = settings.getSpecificationValidators().stream()
                .map(Plugins::instantiate)
                .collect(Collectors.toCollection(() -> new ArrayList<SpecificationValidator>()));
        if (!settings.isAutomaticPluginDiscoveryEnabled()) {
            return providers;
        }
        providers.addAll(discoverServiceImplementations(SpecificationValidator.class));
        return providers;
    }

    static List<SourceProvider<? extends Source>> loadSourceProviders(ImportSpecificationSettings settings) {
        var providers = settings.getSourceProviders().stream()
                .map(Plugins::instantiate)
                .collect(Collectors.toCollection(() -> new ArrayList<SourceProvider<? extends Source>>()));
        if (!settings.isAutomaticPluginDiscoveryEnabled()) {
            return providers;
        }
        providers.addAll(discoverServiceImplementations(SourceProvider.class));
        return providers;
    }

    static List<ActionProvider<? extends Action>> loadActionProviders(ImportSpecificationSettings settings) {
        var providers = settings.getActionProviders().stream()
                .map(Plugins::instantiate)
                .collect(Collectors.toCollection(() -> new ArrayList<ActionProvider<? extends Action>>()));
        if (!settings.isAutomaticPluginDiscoveryEnabled()) {
            return providers;
        }
        providers.addAll(discoverServiceImplementations(ActionProvider.class));
        return providers;
    }

    @SuppressWarnings("unchecked")
    static Set<Class<? extends SpecificationValidator>> loadBuiltInSpecificationValidators() {
        try (var stream = Plugins.class.getResourceAsStream("/import-spec-validators.builtin")) {
            if (stream == null) {
                throw new RuntimeException("Unable to discover built-in specification validators");
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .map(type -> (Class<? extends SpecificationValidator>) loadClass(type))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while discovering built-in specification validators", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> discoverServiceImplementations(Class<?> type) {
        return ServiceLoader.load(type).stream()
                .map(provider -> (T) provider.get())
                .collect(Collectors.toList());
    }

    private static <T> T instantiate(Class<? extends T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(String.format("Unable to instantiate plugin class: %s", type), e);
        }
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Unable to load plugin class: %s", name), e);
        }
    }
}
