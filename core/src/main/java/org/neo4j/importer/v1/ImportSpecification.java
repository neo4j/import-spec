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

/**
 * {@link ImportSpecification} is the direct representation of the import-spec configuration YAML/JSON files.<br>
 * {@link ImportSpecification} is made of 3 mandatory attributes:
 * <ul>
 *  <li>version</li>
 *  <li>a list of {@link Source}</li>
 *  <li>targets, which is a mix of:
 *      <ul>
 *          <li>{@link org.neo4j.importer.v1.targets.NodeTarget}</li>
 *          <li>{@link org.neo4j.importer.v1.targets.RelationshipTarget}</li>
 *          <li>{@link org.neo4j.importer.v1.targets.CustomQueryTarget}</li>
 *      </ul>
 *  </li></ul>
 * {@link ImportSpecification} can also include:
 * <ul>
 *     <li>top-level settings, wrapped into {@link Configuration}</li>
 *     <li>one-off {@link Action} scripts</li>
 * </ul><br>
 * Note: It is <strong>strongly</strong> discouraged to instantiate this class directly.
 * Please call one of the deserialization APIs of {@link ImportSpecificationDeserializer} instead.
 * The deserializer automatically loads all built-in and external
 * {@link org.neo4j.importer.v1.validation.SpecificationValidator} implementation and runs them against this.<br>
 * The direct use of {@link ImportSpecification} is most suited when targeting import tools that fully manage import
 * task ordering such as neo4j-admin (neo4j-admin handles the order between schema initialization, node import and
 * relationship import itself).<br>
 * In other cases, {@link org.neo4j.importer.v1.pipeline.ImportPipeline} is a better choice.<br>
 * You can construct an {@link org.neo4j.importer.v1.pipeline.ImportPipeline} instance from an instance of
 * {@link ImportSpecification} with {@link org.neo4j.importer.v1.pipeline.ImportPipeline#of(ImportSpecification)}.
 */
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

    /**
     * Returns the {@link ImportSpecification} version
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the {@link ImportSpecification} top-level settings
     * @return the top-level settings
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the {@link ImportSpecification} data sources
     * @return the data sources
     */
    public List<Source> getSources() {
        return sources;
    }

    /**
     * Returns the {@link ImportSpecification} node, relationship and custom query targets, wrapped into {@link Targets}
     * @return the targets
     */
    public Targets getTargets() {
        return targets;
    }

    /**
     * Returns the {@link ImportSpecification} actions, active or not
     * @return the actions
     */
    public List<Action> getActions() {
        return actions != null ? actions : Collections.emptyList();
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
