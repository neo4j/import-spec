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
package org.neo4j.importer.v1.cypher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStreamReader;
import java.io.Reader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;

class CypherStatementsTest {

    @Test
    void generates_graph_type_statement_for_node_with_properties() throws Exception {
        var spec = read("/specs/graph-type/one-node.yaml", ImportSpecificationDeserializer::deserialize);

        var result = CypherStatements.generateGraphType(spec);

        assertThat(result)
                .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                        + "\t(n1:Movie => :VisualArt&WorkOfArt {identifier :: ANY NOT NULL IS KEY, title :: STRING IS UNIQUE, embeddings :: VECTOR<FLOAT>(42)})\n"
                        + "}");
    }

    @Test
    void generates_graph_type_statement_for_node_with_composite_constraints() throws Exception {
        var spec = read("/specs/graph-type/one-node-composite.yaml", ImportSpecificationDeserializer::deserialize);

        var result = CypherStatements.generateGraphType(spec);

        assertThat(result)
                .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                        + "\t(n1:Movie => {identifier1 :: STRING, identifier2 :: STRING, title :: STRING, release_year :: INTEGER})\n"
                        + "\t\tREQUIRE (n1.identifier1, n1.identifier2) IS KEY\n"
                        + "\t\tREQUIRE (n1.title, n1.release_year) IS UNIQUE\n"
                        + "}");
    }

    @Test
    void generates_graph_type_statement_for_spec_with_relationship() throws Exception {
        var spec = read("/specs/graph-type/one-rel.yaml", ImportSpecificationDeserializer::deserialize);

        var result = CypherStatements.generateGraphType(spec);

        assertThat(result)
                .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                        + "\t(n1:Movie => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(n2:Actor => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(:Actor)-[r1:ACTED_IN => {}]->(:Movie)\n"
                        + "}");
    }

    @Test
    void generates_graph_type_statement_for_spec_with_relationship_with_properties() throws Exception {
        var spec = read("/specs/graph-type/one-rel-props.yaml", ImportSpecificationDeserializer::deserialize);

        var result = CypherStatements.generateGraphType(spec);

        assertThat(result)
                .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                        + "\t(n1:Movie => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(n2:Actor => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(:Actor)-[r1:ACTED_IN => {role :: STRING NOT NULL, income :: ANY NOT NULL}]->(:Movie)\n"
                        + "}");
    }

    @Test
    void generates_graph_type_statement_for_spec_with_relationship_with_composite_constraints() throws Exception {
        var spec = read("/specs/graph-type/one-rel-composite.yaml", ImportSpecificationDeserializer::deserialize);

        var result = CypherStatements.generateGraphType(spec);

        assertThat(result)
                .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                        + "\t(n1:Movie => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(n2:Actor => {identifier :: ANY NOT NULL IS KEY}),\n"
                        + "\t(:Actor)-[r1:ACTED_IN => {role :: STRING, role_salary :: INTEGER}]->(:Movie)\n"
                        + "\t\tREQUIRE (r1.role, r1.role_salary) IS UNIQUE\n"
                        + "}");
    }

    @Test
    void rejects_spec_without_enabled_graph_type_feature() throws Exception {
        var spec = read("/specs/graph-type/invalid.yaml", ImportSpecificationDeserializer::deserialize);

        assertThatThrownBy(() -> CypherStatements.generateGraphType(spec))
                .isInstanceOf(CypherGenerationPreconditionException.class)
                .hasMessage("Please set 'enable_graph_type' to true in the 'config' section");
    }

    private <T> T read(String classpathResource, ThrowingFunction<Reader, T> fn) throws Exception {
        var stream = this.getClass().getResourceAsStream(classpathResource);
        assertThat(stream).isNotNull();
        try (var reader = new InputStreamReader(stream)) {
            return fn.apply(reader);
        }
    }

    public interface ThrowingFunction<I, O> {
        O apply(I t) throws Exception;
    }
}
