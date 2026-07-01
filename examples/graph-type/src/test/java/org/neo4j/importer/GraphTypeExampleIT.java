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
package org.neo4j.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.cypher.CypherStatements;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.sources.SourceProvider;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class GraphTypeExampleIT {

    static {
        LogManager.getLogManager().reset();
        Logger.getLogger("").setLevel(Level.OFF);
    }

    @Container
    public static Neo4jContainer NEO4J = new Neo4jContainer(DockerImageName.parse("neo4j:2026-enterprise"))
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword("letmein!");

    private static Driver driver;

    @BeforeAll
    static void prepare() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        driver.verifyConnectivity();
    }

    @AfterAll
    static void cleanUp() {
        driver.close();
    }

    @Test
    void generates_graph_type() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/specs/dvd_rental.yaml")) {
            assertThat(stream).isNotNull();
            try (var reader = new InputStreamReader(stream)) {
                var spec = ImportSpecificationDeserializer.deserialize(reader);

                var graphTypeStatement = CypherStatements.generateGraphType(spec);
                driver.executableQuery(graphTypeStatement).execute();

                driver.executableQuery("CREATE (actor:Actor {id: 42, first_name: 'Florent', last_name: 'Biville'}), "
                                + "       (category:Category:Genre {id: 42, name: 'Documentary'}), "
                                + "       (movie:Movie:Film {id: 42,"
                                + "                          title: 'Working at Neo4j', "
                                + "                          description: 'Work life inside the graph of all graphs'}), "
                                + "       (actor)-[:ACTED_IN]->(movie), "
                                + "       (movie)-[:IN_CATEGORY]->(category)")
                        .execute();
                // let's exercise some graph type constraints now
                assertThatThrownBy(() -> driver.executableQuery("CREATE (:Category {id: 43, name: 'Horror'})")
                                .execute())
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("label Category is required to have label Genre");
                assertThatThrownBy(() -> driver.executableQuery("CREATE (:Movie {id: 43, "
                                        + "               title: 'Crafting a recursive SQL query', "
                                        + "               description: 'After so much Cypher, can he still SQL?'"
                                        + "})")
                                .execute())
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("label Movie is required to have label Film");
                assertThatThrownBy(() -> driver.executableQuery(
                                        "CREATE (category:Category:Genre {id: 43, name: 'Horror'}), "
                                                + "       (movie:Movie:Film {id: 43, "
                                                + "                          title: 'Crafting a recursive SQL query', "
                                                + "                          description: 'After so much Cypher, can he still SQL?'"
                                                + "}),"
                                                +
                                                // wrong node labels for the relationship
                                                "       (category)-[:IN_CATEGORY]->(movie)")
                                .execute())
                        .isInstanceOf(ClientException.class)
                        .hasMessageContainingAll("type IN_CATEGORY requires", "start", "to have label Movie");
            }
        }
    }

    public static class ParquetSourceProvider implements SourceProvider<ParquetSource> {

        @Override
        public String supportedType() {
            return "parquet";
        }

        @Override
        public ParquetSource apply(ObjectNode objectNode) {
            return new ParquetSource(
                    objectNode.get("name").textValue(), objectNode.get("uri").textValue());
        }
    }

    public record ParquetSource(String name, String uri) implements Source {

        @Override
        public String getType() {
            return "parquet";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ParquetSource)) return false;
            ParquetSource that = (ParquetSource) o;
            return Objects.equals(name, that.name) && Objects.equals(uri, that.uri);
        }
    }
}
