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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class GraphTypeIT {

    static {
        LogManager.getLogManager().reset();
        Logger.getLogger("").setLevel(Level.OFF);
    }

    @Container
    private static final Neo4jContainer NEO4J = new Neo4jContainer(DockerImageName.parse("neo4j:2026.05.0-enterprise"))
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

    @TestFactory
    Stream<DynamicTest> creates_graph_type() {
        return listSpecs()
                .map(file ->
                        DynamicTest.dynamicTest(String.format("creates graph type from %s", file.getName()), () -> {
                            try (var reader = new FileReader(file)) {
                                var spec = ImportSpecificationDeserializer.deserialize(reader);
                                var graphTypeStatement = CypherStatements.generateGraphType(spec);

                                assertThatCode(() -> driver.executableQuery(graphTypeStatement)
                                                .execute())
                                        .doesNotThrowAnyException();
                            }
                        }));
    }

    private static Stream<File> listSpecs() {
        var folder = specFolder();
        assertThat(folder).isDirectory().isDirectoryContaining("glob:**.yaml");
        var specs = folder.listFiles(file -> file.isFile()
                && !file.getName().startsWith("invalid")
                && file.getName().endsWith(".yaml"));
        assertThat(specs).isNotNull();
        return Arrays.stream(specs);
    }

    private static File specFolder() {
        try {
            var resource = GraphTypeIT.class.getResource("/specs/graph-type/");
            assertThat(resource).isNotNull();
            return new File(resource.toURI());
        } catch (URISyntaxException e) {
            Assertions.fail("Could not resolve spec folder", e);
            throw new RuntimeException("beep beep");
        }
    }
}
