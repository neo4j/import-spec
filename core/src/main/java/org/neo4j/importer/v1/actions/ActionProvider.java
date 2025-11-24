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
package org.neo4j.importer.v1.actions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;

/**
 * {@link ActionProvider} is a Service Provider Interface, allowing users to define their own action.
 * @param <T> the concrete type of the {@link Action} subclass
 */
public interface ActionProvider<T extends Action> extends Function<ObjectNode, T> {

    /**
     * User-facing type of the concrete supported action, it must match the result of {@link Action#getType()}
     * @return the action type
     */
    String supportedType();

    /**
     * Deserialization endpoint.<br>
     * Note: implement deserialization leniently, validate strictly (define
     * {@link org.neo4j.importer.v1.validation.SpecificationValidator} for your own sources)
     * @return the deserialized action
     */
    @Override
    T apply(ObjectNode node);
}
