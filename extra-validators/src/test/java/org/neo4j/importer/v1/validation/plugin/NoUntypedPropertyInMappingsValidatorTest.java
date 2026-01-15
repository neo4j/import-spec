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
import static org.neo4j.importer.v1.targets.PropertyType.STRING;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.targets.*;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;

class NoUntypedPropertyInMappingsValidatorTest {

    @Test
    void fails_validation_for_untyped_node_target_property_mapping() {
        // Given a node target with untyped property mapping
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.CREATE,
                (ObjectNode) null,
                List.of("L1"),
                List.of(new PropertyMapping("id", "id", null)),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoUntypedPropertyInMappingsValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).hasSize(1);

        var error = validationResult.getErrors().iterator().next();

        assertThat(error.getCode()).isEqualTo("TYPE-002");
        assertThat(error.getElementPath()).isEqualTo("$.targets.nodes[0].properties[0].target_property_type");
        assertThat(error.getMessage())
                .isEqualTo(
                        "$.targets.nodes[0].properties[0].target_property_type \"id\" refers to an untyped property");
    }

    @Test
    void fails_validation_for_untyped_relationship_target_property_mapping() {
        // Given a relationship target with untyped property mapping
        var relationshipTarget = new RelationshipTarget(
                true,
                "my-minimal-relationship-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                (ObjectNode) null,
                new NodeReference("my-minimal-node-target"),
                new NodeReference("my-minimal-node-target"),
                List.of(new PropertyMapping("field_1", "property1", null)),
                null);

        // When the relationship is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoUntypedPropertyInMappingsValidator();
        validator.visitRelationshipTarget(0, relationshipTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).hasSize(1);

        var error = validationResult.getErrors().iterator().next();

        assertThat(error.getCode()).isEqualTo("TYPE-002");
        assertThat(error.getElementPath()).isEqualTo("$.targets.relationships[0].properties[0].target_property_type");
        assertThat(error.getMessage())
                .isEqualTo(
                        "$.targets.relationships[0].properties[0].target_property_type \"property1\" refers to an untyped property");
    }

    @Test
    void succeeds_validation_for_typed_node_target_property_mapping() {
        // Given a node target with typed property mapping
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.CREATE,
                (ObjectNode) null,
                List.of("L1"),
                List.of(new PropertyMapping("id", "id", STRING)),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoUntypedPropertyInMappingsValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();

        assertThat(validationResult.passes()).isTrue();
    }

    @Test
    void succeeds_validation_for_typed_relationship_target_property_mapping() {
        // Given a relationship target with typed property mapping
        var relationshipTarget = new RelationshipTarget(
                true,
                "my-minimal-relationship-target",
                "a-source",
                null,
                "TYPE",
                WriteMode.CREATE,
                NodeMatchMode.MATCH,
                (ObjectNode) null,
                new NodeReference("my-minimal-node-target"),
                new NodeReference("my-minimal-node-target"),
                List.of(new PropertyMapping("field_1", "property1", STRING)),
                null);

        // When the relationship is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoUntypedPropertyInMappingsValidator();
        validator.visitRelationshipTarget(0, relationshipTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).isEmpty();
        assertThat(validationResult.passes()).isTrue();
    }
}
