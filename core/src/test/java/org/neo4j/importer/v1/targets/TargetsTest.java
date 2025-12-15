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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.importer.v1.SpecFormat;

class TargetsTest {

    private final YAMLMapper mapper = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    @ParameterizedTest
    @EnumSource(SpecFormat.class)
    void deserializes_targets(SpecFormat format, TestInfo testInfo) throws Exception {

        try (var reader = targetsReader(format, testInfo)) {
            Targets targets = mapper.readValue(reader, Targets.class);

            assertThat(targets.getNodes())
                    .isEqualTo(List.of(new NodeTarget(
                            true,
                            "my-minimal-node-target",
                            "a-source",
                            null,
                            WriteMode.CREATE,
                            (ObjectNode) null,
                            List.of("Label1", "Label2"),
                            List.of(
                                    new PropertyMapping("field_1", "property1", null),
                                    new PropertyMapping("field_2", "property2", null)),
                            null)));
            assertThat(targets.getRelationships())
                    .isEqualTo(List.of(new RelationshipTarget(
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
                            List.of(
                                    new PropertyMapping("field_1", "property1", null),
                                    new PropertyMapping("field_2", "property2", null)),
                            null)));
            assertThat(targets.getCustomQueries())
                    .isEqualTo(List.of(new CustomQueryTarget(
                            true,
                            "my-minimal-custom-query-target",
                            "a-source",
                            null,
                            "UNWIND $rows AS row CREATE (n:ANode) SET n = row")));
        }
    }

    private static FileReader targetsReader(SpecFormat format, TestInfo testInfo) {
        var spec = String.format(
                "/specs/targets_test/%s.%s",
                testInfo.getTestMethod().orElseThrow().getName(), format.extension());
        var resourceUrl = TargetTest.class.getResource(spec);
        assertThat(resourceUrl).isNotNull();
        try {
            return new FileReader(Path.of(resourceUrl.toURI()).toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
