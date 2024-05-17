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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize;

import java.io.StringReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.UndeserializableSourceException;

// This exercises the compliance of various import spec payloads with the JSON schema
// The class focuses on (lack of) compliance of the source side of the spec.
public class ImportSpecificationDeserializerSourceTest {

    @BeforeEach
    void setUp() {
        Assertions.setMaxStackTraceElementsDisplayed(1000);
    }

    @Test
    void fails_if_source_is_missing_type() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0]: required property 'type' not found");
    }

    @Test
    void fails_if_source_type_is_wrongly_typed() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": 42,
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(InvalidSpecificationException.class)
                .hasMessageContainingAll("0 warning(s)", "$.sources[0].type: integer found, string expected");
    }

    @Test
    void fails_if_source_type_is_not_supported_by_any_loaded_source_provider() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "unsupported",
                "query": "SELECT id, name FROM my.table"
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Expected exactly one source provider for sources of type unsupported, but found: 0");
    }

    @Test
    void fails_if_third_party_source_and_supplier_do_not_match() {
        assertThatThrownBy(() -> deserialize(new StringReader(
                        """
        {
            "version": "1",
            "sources": [{
                "name": "a-source",
                "type": "jdbc",
                "sql": "SELECT p.productname FROM products p ORDER BY p.productname ASC "
            }],
            "targets": {
                "queries": [{
                    "name": "a-target",
                    "source": "a-source",
                    "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
                }]
            }
        }
        """
                                .stripIndent())))
                .isInstanceOf(UndeserializableSourceException.class)
                .hasMessageContainingAll(
                        "Source provider org.neo4j.importer.v1.sources.JdbcSourceProvider failed to deserialize the following source definition");
    }
}
