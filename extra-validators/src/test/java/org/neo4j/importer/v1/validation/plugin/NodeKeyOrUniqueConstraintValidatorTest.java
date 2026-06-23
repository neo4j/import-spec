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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.NodeUniqueConstraint;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;

class NodeKeyOrUniqueConstraintValidatorTest {

    @Test
    void fails_validation_for_merge_node_target_without_key_or_unique_constraint() {
        // Given a merge node target without key or unique constraints
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.MERGE,
                (ObjectNode) null,
                List.of("Label"),
                List.of(new PropertyMapping("id", "id", null)),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NodeKeyOrUniqueConstraintValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).hasSize(1);

        var error = validationResult.getErrors().iterator().next();
        assertThat(error.getCode()).isEqualTo("NKUC-001");
        assertThat(error.getElementPath()).isEqualTo("$.targets.nodes[0].schema");
        assertThat(error.getMessage())
                .isEqualTo(
                        "Node target 'a-node-target' with write_mode merge requires at least one node key constraint or unique constraint, none found");
    }

    @Test
    void succeeds_validation_for_merge_node_target_with_key_constraint() {
        // Given a merge node target with a key constraint
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.MERGE,
                (ObjectNode) null,
                List.of("Label"),
                List.of(new PropertyMapping("id", "id", null)),
                new NodeSchema(
                        null,
                        List.of(new NodeKeyConstraint("node_key", "Label", List.of("id"), null)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NodeKeyOrUniqueConstraintValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.passes()).isTrue();
    }

    @Test
    void succeeds_validation_for_merge_node_target_with_unique_constraint() {
        // Given a merge node target with a unique constraint
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.MERGE,
                (ObjectNode) null,
                List.of("Label"),
                List.of(new PropertyMapping("id", "id", null)),
                new NodeSchema(
                        null,
                        null,
                        List.of(new NodeUniqueConstraint("node_unique", "Label", List.of("id"), null)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NodeKeyOrUniqueConstraintValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.passes()).isTrue();
    }

    @Test
    void succeeds_validation_for_create_mode_node_target_without_constraints() {
        // Given a create node target without constraints
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.CREATE,
                (ObjectNode) null,
                List.of("Label"),
                List.of(new PropertyMapping("id", "id", null)),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NodeKeyOrUniqueConstraintValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.passes()).isTrue();
    }

    @Test
    void succeeds_validation_for_inactive_merge_node_target_without_constraints() {
        // Given an inactive merge node target without constraints
        var nodeTarget = new NodeTarget(
                false,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.MERGE,
                (ObjectNode) null,
                List.of("Label"),
                List.of(new PropertyMapping("id", "id", null)),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NodeKeyOrUniqueConstraintValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.passes()).isTrue();
    }
}
