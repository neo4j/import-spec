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
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.WriteMode;
import org.neo4j.importer.v1.validation.SpecificationValidationResult;

class NoInvalidCharsInTypeAndLabelValidatorTest {

    @Test
    void fails_validation_for_invalid_label_chars() {
        // Given a node with labels few one that contains invalid char
        var nodeTarget = new NodeTarget(
                true,
                "a-node-target",
                "my-bigquery-source",
                null,
                WriteMode.CREATE,
                (ObjectNode) null,
                List.of("OrdinaryLabel", "LabelWith:Colon", "LabelWith=Sign", "AnotherValidLabel"),
                List.of(),
                null);

        // When the node is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoInvalidCharsInTypeAndLabelValidator();
        validator.visitNodeTarget(0, nodeTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).hasSize(2);

        var errors = validationResult.getErrors().iterator();
        var firstError = errors.next();
        assertThat(firstError.getCode()).isEqualTo("CHAR-001");
        assertThat(firstError.getElementPath()).isEqualTo("$.targets.nodes[0].labels[1]");
        assertThat(firstError.getMessage())
                .isEqualTo("$.targets.nodes[0].labels[1] \"LabelWith:Colon\" contains invalid character");

        var secondError = errors.next();
        assertThat(secondError.getCode()).isEqualTo("CHAR-001");
        assertThat(secondError.getElementPath()).isEqualTo("$.targets.nodes[0].labels[2]");
        assertThat(secondError.getMessage())
                .isEqualTo("$.targets.nodes[0].labels[2] \"LabelWith=Sign\" contains invalid character");
    }

    @Test
    void fails_validation_for_invalid_relationship_type_chars() {
        // Given a relationship with a type containing invalid char
        var relationshipTarget = new RelationshipTarget(
                true,
                "a-relationship-target",
                "my-bigquery-source",
                null,
                "TypeWith=Sign",
                WriteMode.CREATE,
                null,
                (List) null,
                new NodeReference("start-node"),
                new NodeReference("end-node"),
                List.of(),
                null);

        // When the relationship is validated
        var builder = new SpecificationValidationResult.Builder();
        var validator = new NoInvalidCharsInTypeAndLabelValidator();
        validator.visitRelationshipTarget(0, relationshipTarget);
        validator.report(builder);
        var validationResult = builder.build();

        // Then
        assertThat(validationResult.getErrors()).hasSize(1);

        var error = validationResult.getErrors().iterator().next();
        assertThat(error.getCode()).isEqualTo("CHAR-001");
        assertThat(error.getElementPath()).isEqualTo("$.targets.relationships[0].type");
        assertThat(error.getMessage())
                .isEqualTo("$.targets.relationships[0].type \"TypeWith=Sign\" contains invalid character");
    }
}
