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
package model.mapping

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object MappingSerializer : JsonContentPolymorphicSerializer<Mapping>(Mapping::class) {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Mapping", PolymorphicKind.SEALED) {
        element("type", String.serializer().descriptor)

        element(
            "value",
            buildClassSerialDescriptor("MappingChoices") {
                element(MappingType.NODE, NodeMapping.serializer().descriptor)
                element(MappingType.RELATIONSHIP, RelationshipMapping.serializer().descriptor)
                element(MappingType.QUERY, QueryMapping.serializer().descriptor)
                element(MappingType.LABEL, LabelMapping.serializer().descriptor)
            }
        )
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Mapping> {
        val jsonObject = element.jsonObject
        return when {
            "node" in jsonObject -> NodeMapping.serializer()
            "relationship" in jsonObject -> RelationshipMapping.serializer()
            "query" in jsonObject -> QueryMapping.serializer()
            "field" in jsonObject -> LabelMapping.serializer()
            else -> throw SerializationException("Unknown Mapping type for object: $jsonObject")
        }
    }
}
