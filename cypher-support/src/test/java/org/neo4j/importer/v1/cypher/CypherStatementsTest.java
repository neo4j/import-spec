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
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;

class CypherStatementsTest {

    @Nested
    class GraphTypeStatementTest {
        @Test
        void generates_statement_for_node_with_properties() throws Exception {
            var spec = read("/specs/graph-type/one-node.yaml", ImportSpecificationDeserializer::deserialize);

            var result = CypherStatements.generateGraphType(spec);

            assertThat(result)
                    .isEqualTo("ALTER CURRENT GRAPH TYPE SET {\n"
                            + "\t(n1:Movie => :VisualArt&WorkOfArt {identifier :: ANY NOT NULL IS KEY, title :: STRING IS UNIQUE, embeddings :: VECTOR<FLOAT>(42)})\n"
                            + "}");
        }

        @Test
        void generates_statement_for_node_with_composite_constraints() throws Exception {
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
        void generates_statement_for_spec_with_relationship() throws Exception {
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
        void generates_statement_for_spec_with_relationship_with_properties() throws Exception {
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
        void generates_statement_for_spec_with_relationship_with_composite_constraints() throws Exception {
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
        void rejects_spec_without_enabled_feature() throws Exception {
            var spec = read("/specs/graph-type/invalid.yaml", ImportSpecificationDeserializer::deserialize);

            assertThatThrownBy(() -> CypherStatements.generateGraphType(spec))
                    .isInstanceOf(CypherGenerationPreconditionException.class)
                    .hasMessage("Please set 'enable_graph_type' to true in the 'config' section");
        }
    }

    @Nested
    class ConstraintStatementTest {
        private static final String basePath = "/specs/constraints/";

        @Test
        void generates_statement_for_simple_spec() throws Exception {
            var spec = read(basePath + "books-simple.yaml", ImportSpecificationDeserializer::deserialize);
            var result = CypherStatements.generateConstraints(spec);

            // spotless:off
            var expected = List.of(
                "CREATE CONSTRAINT `book-id-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `book-isbn-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS :: STRING",
                "CREATE CONSTRAINT `book-title-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS :: STRING",
                "CREATE CONSTRAINT `book-summary-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`summary` IS :: STRING",
                "CREATE CONSTRAINT `book-id-key` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`id`) IS NODE KEY",
                "CREATE CONSTRAINT `book-isbn-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS NOT NULL",
                "CREATE CONSTRAINT `book-title-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS NOT NULL",
                "CREATE CONSTRAINT `book-isbn-unique` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`isbn`) IS UNIQUE",
                "CREATE CONSTRAINT `author-id-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `author-registration-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS :: INTEGER",
                "CREATE CONSTRAINT `author-full-name-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS :: STRING",
                "CREATE CONSTRAINT `author-initials-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS :: STRING",
                "CREATE CONSTRAINT `author-first-published-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS :: DATE",
                "CREATE CONSTRAINT `author-id-key` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`id`) IS NODE KEY",
                "CREATE CONSTRAINT `author-registration-number-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS NOT NULL",
                "CREATE CONSTRAINT `author-full-name-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS NOT NULL",
                "CREATE CONSTRAINT `author-initials-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS NOT NULL",
                "CREATE CONSTRAINT `author-first-published-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS NOT NULL",
                "CREATE CONSTRAINT `author-registration-unique` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`registrationNumber`) IS UNIQUE",
                "CREATE CONSTRAINT `published-issue-id-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `published-publisher-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS :: STRING",
                "CREATE CONSTRAINT `published-catalog-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS :: STRING",
                "CREATE CONSTRAINT `published-first-issued-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS :: DATE",
                "CREATE CONSTRAINT `published-history-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`history` IS :: LIST<DATE NOT NULL>",
                "CREATE CONSTRAINT `published-id-key` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`id`) IS RELATIONSHIP KEY",
                "CREATE CONSTRAINT `published-publisher-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS NOT NULL",
                "CREATE CONSTRAINT `published-catalog-id-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS NOT NULL",
                "CREATE CONSTRAINT `published-first-issued-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS NOT NULL",
                "CREATE CONSTRAINT `published-catalog-id-unique` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`catalogId`) IS UNIQUE"
            );
            // spotless:on

            assertThat(result).containsExactlyElementsOf(expected);
        }

        @Test
        void generates_statement_with_options_for_simple_spec() throws Exception {
            var spec = read(basePath + "books-simple-options.yaml", ImportSpecificationDeserializer::deserialize);
            var result = CypherStatements.generateConstraints(spec);

            // spotless:off
            var expected = List.of(
                "CREATE CONSTRAINT `book-id-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `book-isbn-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS :: STRING",
                "CREATE CONSTRAINT `book-title-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS :: STRING",
                "CREATE CONSTRAINT `book-summary-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`summary` IS :: STRING",
                "CREATE CONSTRAINT `book-id-key` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`id`) IS NODE KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `book-isbn-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS NOT NULL",
                "CREATE CONSTRAINT `book-title-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS NOT NULL",
                "CREATE CONSTRAINT `book-isbn-unique` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`isbn`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `author-id-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `author-registration-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS :: INTEGER",
                "CREATE CONSTRAINT `author-full-name-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS :: STRING",
                "CREATE CONSTRAINT `author-initials-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS :: STRING",
                "CREATE CONSTRAINT `author-first-published-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS :: DATE",
                "CREATE CONSTRAINT `author-id-key` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`id`) IS NODE KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `author-registration-number-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS NOT NULL",
                "CREATE CONSTRAINT `author-full-name-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS NOT NULL",
                "CREATE CONSTRAINT `author-initials-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS NOT NULL",
                "CREATE CONSTRAINT `author-first-published-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS NOT NULL",
                "CREATE CONSTRAINT `author-registration-unique` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`registrationNumber`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `published-issue-id-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `published-publisher-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS :: STRING",
                "CREATE CONSTRAINT `published-catalog-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS :: STRING",
                "CREATE CONSTRAINT `published-first-issued-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS :: DATE",
                "CREATE CONSTRAINT `published-history-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`history` IS :: LIST<DATE NOT NULL>",
                "CREATE CONSTRAINT `published-id-key` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`id`) IS RELATIONSHIP KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `published-publisher-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS NOT NULL",
                "CREATE CONSTRAINT `published-catalog-id-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS NOT NULL",
                "CREATE CONSTRAINT `published-first-issued-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS NOT NULL",
                "CREATE CONSTRAINT `published-catalog-id-unique` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`catalogId`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }"
            );
            // spotless:on

            assertThat(result).containsExactlyElementsOf(expected);
        }

        @Test
        void generates_statement_for_composite_spec() throws Exception {
            var spec = read(basePath + "books-composite.yaml", ImportSpecificationDeserializer::deserialize);
            var result = CypherStatements.generateConstraints(spec);

            // spotless:off
            var expected = List.of(
                "CREATE CONSTRAINT `book-id-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `book-isbn-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS :: STRING",
                "CREATE CONSTRAINT `book-title-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS :: STRING",
                "CREATE CONSTRAINT `book-summary-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`summary` IS :: STRING",
                "CREATE CONSTRAINT `book-id-and-isbn-key` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`id`, n.`isbn`) IS NODE KEY",
                "CREATE CONSTRAINT `book-title-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS NOT NULL",
                "CREATE CONSTRAINT `book-isbn-and-title-unique` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`isbn`, n.`title`) IS UNIQUE",
                "CREATE CONSTRAINT `author-id-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `author-registration-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS :: INTEGER",
                "CREATE CONSTRAINT `author-full-name-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS :: STRING",
                "CREATE CONSTRAINT `author-initials-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS :: STRING",
                "CREATE CONSTRAINT `author-first-published-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS :: DATE",
                "CREATE CONSTRAINT `author-id-and-registration-key` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`id`, n.`registrationNumber`) IS NODE KEY",
                "CREATE CONSTRAINT `author-full-name-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS NOT NULL",
                "CREATE CONSTRAINT `author-initials-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS NOT NULL",
                "CREATE CONSTRAINT `author-first-published-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS NOT NULL",
                "CREATE CONSTRAINT `author-registration-and-name-unique` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`registrationNumber`, n.`fullName`) IS UNIQUE",
                "CREATE CONSTRAINT `published-issue-id-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `published-publisher-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS :: STRING",
                "CREATE CONSTRAINT `published-catalog-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS :: STRING",
                "CREATE CONSTRAINT `published-first-issued-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS :: DATE",
                "CREATE CONSTRAINT `published-history-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`history` IS :: LIST<DATE NOT NULL>",
                "CREATE CONSTRAINT `published-id-and-publisher-key` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`id`, r.`publisher`) IS RELATIONSHIP KEY",
                "CREATE CONSTRAINT `published-catalog-id-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS NOT NULL",
                "CREATE CONSTRAINT `published-first-issued-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS NOT NULL",
                "CREATE CONSTRAINT `published-publisher-and-catalog-id-unique` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`publisher`, r.`catalogId`) IS UNIQUE"
            );
            // spotless:on

            assertThat(result).containsExactlyElementsOf(expected);
        }

        @Test
        void generates_statement_with_options_for_composite_spec() throws Exception {
            var spec = read(basePath + "books-composite-options.yaml", ImportSpecificationDeserializer::deserialize);
            var result = CypherStatements.generateConstraints(spec);

            // spotless:off
            var expected = List.of(
                "CREATE CONSTRAINT `book-id-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `book-isbn-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`isbn` IS :: STRING",
                "CREATE CONSTRAINT `book-title-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS :: STRING",
                "CREATE CONSTRAINT `book-summary-type` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`summary` IS :: STRING",
                "CREATE CONSTRAINT `book-id-and-isbn-key` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`id`, n.`isbn`) IS NODE KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `book-title-must-exist` IF NOT EXISTS FOR (n:`Book`) REQUIRE n.`title` IS NOT NULL",
                "CREATE CONSTRAINT `book-isbn-and-title-unique` IF NOT EXISTS FOR (n:`Book`) REQUIRE (n.`isbn`, n.`title`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `author-id-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `author-registration-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`registrationNumber` IS :: INTEGER",
                "CREATE CONSTRAINT `author-full-name-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS :: STRING",
                "CREATE CONSTRAINT `author-initials-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS :: STRING",
                "CREATE CONSTRAINT `author-first-published-type` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS :: DATE",
                "CREATE CONSTRAINT `author-id-and-registration-key` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`id`, n.`registrationNumber`) IS NODE KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `author-full-name-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`fullName` IS NOT NULL",
                "CREATE CONSTRAINT `author-initials-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`initials` IS NOT NULL",
                "CREATE CONSTRAINT `author-first-published-must-exist` IF NOT EXISTS FOR (n:`Author`) REQUIRE n.`firstPublished` IS NOT NULL",
                "CREATE CONSTRAINT `author-registration-and-name-unique` IF NOT EXISTS FOR (n:`Author`) REQUIRE (n.`registrationNumber`, n.`fullName`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `published-issue-id-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`id` IS :: INTEGER",
                "CREATE CONSTRAINT `published-publisher-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`publisher` IS :: STRING",
                "CREATE CONSTRAINT `published-catalog-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS :: STRING",
                "CREATE CONSTRAINT `published-first-issued-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS :: DATE",
                "CREATE CONSTRAINT `published-history-type` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`history` IS :: LIST<DATE NOT NULL>",
                "CREATE CONSTRAINT `published-id-and-publisher-key` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`id`, r.`publisher`) IS RELATIONSHIP KEY OPTIONS { indexProvider: 'range-1.0' }",
                "CREATE CONSTRAINT `published-catalog-id-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`catalogId` IS NOT NULL",
                "CREATE CONSTRAINT `published-first-issued-must-exist` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE r.`firstIssued` IS NOT NULL",
                "CREATE CONSTRAINT `published-publisher-and-catalog-id-unique` IF NOT EXISTS FOR ()-[r:`PUBLISHED`]-() REQUIRE (r.`publisher`, r.`catalogId`) IS UNIQUE OPTIONS { indexProvider: 'range-1.0' }"
            );
            // spotless:on

            assertThat(result).containsExactlyElementsOf(expected);
        }
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
