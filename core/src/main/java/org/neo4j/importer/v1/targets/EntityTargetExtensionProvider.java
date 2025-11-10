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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;

/**
 * {@link EntityTargetExtensionProvider} is a Service Provider Interface, allowing users to define their own entity
 * (node, relationship) target extension data.<br>
 * The deserialized extensions can then be retrieved with:
 * <ul>
 * <li>{@link NodeTarget#getExtensions()} or {@link RelationshipTarget#getExtensions()}</li>
 * <li>{@link NodeTarget#getExtension(Class)} or {@link RelationshipTarget#getExtension(Class)}</li>
 * </ul>
 * @param <T> the concrete type of the {@link EntityTargetExtension} subclass
 */
public interface EntityTargetExtensionProvider<T extends EntityTargetExtension> extends Function<ObjectNode, T> {}
