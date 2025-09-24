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

import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionProvider;
import org.neo4j.importer.v1.actions.plugin.CypherActionProvider;
import org.neo4j.importer.v1.distribution.Neo4jDistribution;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.neo4j.importer.v1.targets.EntityTargetExtension;
import org.neo4j.importer.v1.targets.EntityTargetExtensionProvider;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class ImportSpecificationSettings {

    public static final ImportSpecificationSettings DEFAULT_SETTINGS = builder().build();

    private final boolean automaticPluginDiscoveryEnabled;
    private final Set<Class<? extends SpecificationValidator>> specificationValidators;
    private final Set<Class<? extends SourceProvider<? extends Source>>> sourceProviders;
    private final Set<Class<? extends ActionProvider<? extends Action>>> actionProviders;
    private final Set<Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>>
            entityTargetExtensionProviders;
    private final Neo4jDistribution neo4jDistribution;

    public ImportSpecificationSettings(
            boolean automaticPluginDiscoveryEnabled,
            Set<Class<? extends SpecificationValidator>> specificationValidators,
            Set<Class<? extends SourceProvider<? extends Source>>> sourceProviders,
            Set<Class<? extends ActionProvider<? extends Action>>> actionProviders,
            Set<Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>>
                    entityTargetExtensionProviders,
            Neo4jDistribution neo4jDistribution) {
        this.automaticPluginDiscoveryEnabled = automaticPluginDiscoveryEnabled;
        this.specificationValidators = unmodifiableSet(specificationValidators);
        this.sourceProviders = unmodifiableSet(sourceProviders);
        this.actionProviders = unmodifiableSet(actionProviders);
        this.entityTargetExtensionProviders = unmodifiableSet(entityTargetExtensionProviders);
        this.neo4jDistribution = neo4jDistribution;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isAutomaticPluginDiscoveryEnabled() {
        return automaticPluginDiscoveryEnabled;
    }

    public Set<Class<? extends SpecificationValidator>> getSpecificationValidators() {
        return specificationValidators;
    }

    public Set<Class<? extends SourceProvider<? extends Source>>> getSourceProviders() {
        return sourceProviders;
    }

    public Set<Class<? extends ActionProvider<? extends Action>>> getActionProviders() {
        return actionProviders;
    }

    public Set<Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>>
            getEntityTargetExtensionProviders() {
        return entityTargetExtensionProviders;
    }

    public Optional<Neo4jDistribution> getNeo4jDistribution() {
        return Optional.ofNullable(neo4jDistribution);
    }

    @Override
    public String toString() {
        return "ImportSpecificationConfig{" + "enableAutomaticPluginDiscovery="
                + automaticPluginDiscoveryEnabled + ", sourceProviders="
                + sourceProviders + ", actionProviders="
                + actionProviders + ", entityTargetExtensionProviders="
                + entityTargetExtensionProviders + ", neo4jDistribution="
                + neo4jDistribution + '}';
    }

    public static class Builder {
        private boolean automaticPluginDiscoveryEnabled = true;
        private final Set<Class<? extends SpecificationValidator>> specificationValidators =
                Plugins.loadBuiltInSpecificationValidators();
        private final Set<Class<? extends SourceProvider<? extends Source>>> sourceProviders = new HashSet<>();
        private final Set<Class<? extends ActionProvider<? extends Action>>> actionProviders =
                new HashSet<>(Set.of(CypherActionProvider.class));
        private final Set<Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>>
                entityTargetExtensionProviders = new HashSet<>();
        private Neo4jDistribution neo4jDistribution;

        private Builder() {}

        public Builder withAutomaticPluginDiscoveryDisabled() {
            this.automaticPluginDiscoveryEnabled = false;
            return this;
        }

        public Builder withAutomaticPluginDiscoveryEnabled() {
            this.automaticPluginDiscoveryEnabled = true;
            return this;
        }

        @SafeVarargs
        public final Builder withSpecificationValidators(
                Class<SpecificationValidator> specificationValidator, Class<SpecificationValidator>... otherProviders) {
            return withSpecificationValidators(concat(specificationValidator, otherProviders));
        }

        public Builder withSpecificationValidators(
                Set<Class<? extends SpecificationValidator>> specificationValidators) {
            this.specificationValidators.addAll(specificationValidators);
            return this;
        }

        @SafeVarargs
        public final Builder withSourceProviders(
                Class<SourceProvider<? extends Source>> sourceProvider,
                Class<SourceProvider<? extends Source>>... otherProviders) {
            return withSourceProviders(concat(sourceProvider, otherProviders));
        }

        public Builder withSourceProviders(Set<Class<? extends SourceProvider<? extends Source>>> sourceProviders) {
            this.sourceProviders.addAll(sourceProviders);
            return this;
        }

        @SafeVarargs
        public final Builder withActionProviders(
                Class<? extends ActionProvider<? extends Action>> actionProvider,
                Class<? extends ActionProvider<? extends Action>>... otherProviders) {
            return withActionProviders(concat(actionProvider, otherProviders));
        }

        public Builder withActionProviders(Set<Class<? extends ActionProvider<? extends Action>>> actionProviders) {
            this.actionProviders.addAll(actionProviders);
            return this;
        }

        @SafeVarargs
        public final Builder withEntityTargetExtensionProviders(
                Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>
                        entityTargetExtensionProvider,
                Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>... otherProviders) {
            return withEntityTargetExtensionProviders(concat(entityTargetExtensionProvider, otherProviders));
        }

        public Builder withEntityTargetExtensionProviders(
                Set<Class<? extends EntityTargetExtensionProvider<? extends EntityTargetExtension>>>
                        entityTargetExtensionProviders) {
            this.entityTargetExtensionProviders.addAll(entityTargetExtensionProviders);
            return this;
        }

        public Builder withRuntimeValidationEnabledAgainst(Neo4jDistribution neo4jDistribution) {
            this.neo4jDistribution = neo4jDistribution;
            return this;
        }

        public ImportSpecificationSettings build() {
            return new ImportSpecificationSettings(
                    automaticPluginDiscoveryEnabled,
                    specificationValidators,
                    sourceProviders,
                    actionProviders,
                    entityTargetExtensionProviders,
                    neo4jDistribution);
        }

        private static <T> Set<T> concat(T sourceProvider, T[] otherProviders) {
            var providers = new LinkedHashSet<T>(1 + otherProviders.length);
            providers.add(sourceProvider);
            providers.addAll(Arrays.asList(otherProviders));
            return providers;
        }
    }
}
