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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeMatchMode;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Targets;
import org.neo4j.importer.v1.targets.WriteMode;

class ImportPipelineTest {

    @Test
    void copies_targets_of_relationship_nodes_when_there_is_no_custom_key_mappings() {
        var simpleStartRef = new NodeReference("actor-nodes");
        var simpleEndRef = new NodeReference("movie-nodes");
        var spec = new ImportSpecification(
                "a-version",
                new HashMap<>(),
                List.of(new DummySource("movies"), new DummySource("actors"), new DummySource("actor-in-movies")),
                new Targets(
                        List.of(
                                new NodeTarget(
                                        true,
                                        "movie-nodes",
                                        "movies",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Movie"),
                                        List.of(new PropertyMapping("id", "movie_id", PropertyType.STRING)),
                                        schemaFor(new NodeKeyConstraint(
                                                "movie_id-key", "Movie", List.of("movie_id"), Map.of()))),
                                new NodeTarget(
                                        true,
                                        "actor-nodes",
                                        "actors",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Actor"),
                                        List.of(new PropertyMapping("id", "actor_id", PropertyType.INTEGER)),
                                        schemaFor(new NodeKeyConstraint(
                                                "actor_id-key", "Actor", List.of("actor_id"), Map.of())))),
                        List.of(new RelationshipTarget(
                                true,
                                "played-relationships",
                                "actor-in-movies",
                                List.of(),
                                "PLAYED_IN",
                                WriteMode.MERGE,
                                NodeMatchMode.MATCH,
                                (ObjectNode) null,
                                // note: this example would actually be bogus in real life,
                                // since the same "id" source field would map to both movie and actor IDs
                                simpleStartRef,
                                simpleEndRef,
                                List.of(),
                                null)),
                        List.of()),
                List.of());

        var pipeline = ImportPipeline.of(spec);

        var relationships = stream(pipeline)
                .filter(step -> step instanceof RelationshipTargetStep)
                .map(step -> (RelationshipTargetStep) step)
                .toList();
        assertThat(relationships).hasSize(1);
        var relationship = relationships.getFirst();
        assertThat(relationship.startNode().keyProperties())
                .containsExactly(new PropertyMapping("id", "actor_id", PropertyType.INTEGER));
        assertThat(relationship.endNode().keyProperties())
                .containsExactly(new PropertyMapping("id", "movie_id", PropertyType.STRING));
    }

    @Test
    void overwrites_key_mappings_for_relationship_node_targets() {
        var startRef = new NodeReference("actor-nodes", List.of(new KeyMapping("actorId", "actor_id")));
        var endRef = new NodeReference("movie-nodes", List.of(new KeyMapping("movieId", "movie_id")));
        var spec = new ImportSpecification(
                "a-version",
                new HashMap<>(),
                List.of(new DummySource("movies"), new DummySource("actors"), new DummySource("actor-in-movies")),
                new Targets(
                        List.of(
                                new NodeTarget(
                                        true,
                                        "movie-nodes",
                                        "movies",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Movie"),
                                        List.of(new PropertyMapping("id", "movie_id", PropertyType.STRING)),
                                        schemaFor(new NodeKeyConstraint(
                                                "movie_id-key", "Movie", List.of("movie_id"), Map.of()))),
                                new NodeTarget(
                                        true,
                                        "actor-nodes",
                                        "actors",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Actor"),
                                        List.of(new PropertyMapping("id", "actor_id", PropertyType.INTEGER)),
                                        schemaFor(new NodeKeyConstraint(
                                                "actor_id-key", "Actor", List.of("actor_id"), Map.of())))),
                        List.of(new RelationshipTarget(
                                true,
                                "played-relationships",
                                "actor-in-movies",
                                List.of(),
                                "PLAYED_IN",
                                WriteMode.MERGE,
                                NodeMatchMode.MATCH,
                                (ObjectNode) null,
                                startRef,
                                endRef,
                                List.of(),
                                null)),
                        List.of()),
                List.of());

        var pipeline = ImportPipeline.of(spec);

        var relationships = stream(pipeline)
                .filter(step -> step instanceof RelationshipTargetStep)
                .map(step -> (RelationshipTargetStep) step)
                .toList();
        assertThat(relationships).hasSize(1);
        var relationship = relationships.getFirst();
        assertThat(relationship.startNode().keyProperties())
                .containsExactly(new PropertyMapping("actorId", "actor_id", PropertyType.INTEGER));
        assertThat(relationship.endNode().keyProperties())
                .containsExactly(new PropertyMapping("movieId", "movie_id", PropertyType.STRING));
    }

    @Test
    void exposes_correct_execution_plan_for_relationship_with_explicit_key_mappings() {
        var startRef = new NodeReference("actor-nodes", List.of(new KeyMapping("actorId", "actor_id")));
        var endRef = new NodeReference("movie-nodes", List.of(new KeyMapping("movieId", "movie_id")));
        var spec = new ImportSpecification(
                "a-version",
                new HashMap<>(),
                List.of(new DummySource("movies"), new DummySource("actors"), new DummySource("actor-in-movies")),
                new Targets(
                        List.of(
                                new NodeTarget(
                                        true,
                                        "movie-nodes",
                                        "movies",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Movie"),
                                        List.of(new PropertyMapping("id", "movie_id", PropertyType.STRING)),
                                        schemaFor(new NodeKeyConstraint(
                                                "movie_id-key", "Movie", List.of("movie_id"), Map.of()))),
                                new NodeTarget(
                                        true,
                                        "actor-nodes",
                                        "actors",
                                        List.of(),
                                        WriteMode.MERGE,
                                        (ObjectNode) null,
                                        List.of("Actor"),
                                        List.of(new PropertyMapping("id", "actor_id", PropertyType.INTEGER)),
                                        schemaFor(new NodeKeyConstraint(
                                                "actor_id-key", "Actor", List.of("actor_id"), Map.of())))),
                        List.of(new RelationshipTarget(
                                true,
                                "played-relationships",
                                "actor-in-movies",
                                List.of(),
                                "PLAYED_IN",
                                WriteMode.MERGE,
                                NodeMatchMode.MATCH,
                                (ObjectNode) null,
                                startRef,
                                endRef,
                                List.of(),
                                null)),
                        List.of()),
                List.of());

        var plan = ImportPipeline.of(spec).executionPlan();

        var groups = plan.getGroups();
        assertThat(groups).hasSize(1);
        var group = groups.getFirst();
        var stages = group.getStages();
        assertThat(stages).hasSize(3);
        assertThat(stages.get(0).getSteps().stream().map(ImportStep::name))
                .containsExactlyInAnyOrder("movies", "actors", "actor-in-movies");
        assertThat(stages.get(1).getSteps().stream().map(ImportStep::name))
                .containsExactlyInAnyOrder("movie-nodes", "actor-nodes");
        assertThat(stages.get(2).getSteps().stream().map(ImportStep::name))
                .containsExactlyInAnyOrder("played-relationships");
    }

    private static @NotNull NodeSchema schemaFor(NodeKeyConstraint keyConstraint) {
        return new NodeSchema(null, List.of(keyConstraint), null, null, null, null, null, null, null);
    }

    private static @NotNull Stream<ImportStep> stream(ImportPipeline pipeline) {
        return StreamSupport.stream(pipeline.spliterator(), false);
    }

    static class DummySource implements Source {

        private final String name;

        public DummySource(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return "dummy";
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
