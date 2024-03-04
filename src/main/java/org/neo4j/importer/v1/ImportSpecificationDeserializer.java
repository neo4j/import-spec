/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.Reader;

public class ImportSpecificationDeserializer {

    private static final YAMLMapper MAPPER = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    public static ImportSpecification deserialize(Reader spec) {
        try {
            return MAPPER.readValue(spec, ImportSpecification.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not deserialize spec %s", spec), e);
        }
    }
}
