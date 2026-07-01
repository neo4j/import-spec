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

/**
 * CypherGenerationPreconditionException describes an issue that prevents the Cypher generation method in
 * {@link CypherStatements} to run.
 */
public class CypherGenerationPreconditionException extends IllegalArgumentException {

    /**
     * Creates an CypherGenerationPreconditionException with the provided message
     * @param message error description
     */
    public CypherGenerationPreconditionException(String message) {
        super(message);
    }
}
