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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.Targets;

public class ImportSpecification implements Serializable {

    private final String version;
    private final Configuration configuration;

    private final List<Source> sources;

    private final Targets targets;

    private final List<Action> actions;

    @JsonCreator
    public ImportSpecification(
            @JsonProperty(value = "version", required = true) String version,
            @JsonProperty("config") Map<String, Object> configuration,
            @JsonProperty(value = "sources", required = true) List<Source> sources,
            @JsonProperty(value = "targets", required = true) Targets targets,
            @JsonProperty("actions") List<Action> actions) {

        this.version = version;
        this.configuration = new Configuration(configuration);
        this.sources = sources;
        this.targets = targets;
        this.actions = actions;
    }

    public String getVersion() {
        return version;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public List<Source> getSources() {
        return sources;
    }

    public Targets getTargets() {
        return targets;
    }

    public List<Action> getActions() {
        return actions != null ? actions : Collections.emptyList();
    }

    public Source findSourceByName(String source) {
        return sources.stream()
                .filter((src) -> src.getName().equals(source))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(String.format("Could not find any source named %s", source)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportSpecification that = (ImportSpecification) o;
        return Objects.equals(version, that.version)
                && Objects.equals(configuration, that.configuration)
                && Objects.equals(sources, that.sources)
                && Objects.equals(targets, that.targets)
                && Objects.equals(actions, that.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, configuration, sources, targets, actions);
    }

    @Override
    public String toString() {
        return "ImportSpecification{" + "version='"
                + version + '\'' + ", configuration="
                + configuration + ", sources="
                + sources + ", targets="
                + targets + ", actions="
                + actions + '}';
    }
}
