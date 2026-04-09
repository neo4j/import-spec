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
package model.extension

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object ExtensionValueSerializer : JsonContentPolymorphicSerializer<ExtensionValue>(ExtensionValue::class) {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ExtensionValue", PolymorphicKind.SEALED) {
        element("type", String.serializer().descriptor)

        element(
            "value",
            buildClassSerialDescriptor("ExtensionValues") {
                element(ExtensionType.STRING, StringValue.serializer().descriptor)
                element(ExtensionType.BOOLEAN, BooleanValue.serializer().descriptor)
                element(ExtensionType.LONG, LongValue.serializer().descriptor)
                element(ExtensionType.DOUBLE, DoubleValue.serializer().descriptor)
                element(ExtensionType.LIST, ListValue.serializer().descriptor)
                element(ExtensionType.MAP, MapValue.serializer().descriptor)
            }
        )
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ExtensionValue> {
        val jsonObject = element.jsonObject
        return when {
            ExtensionType.STRING in jsonObject -> StringValue.serializer()
            ExtensionType.BOOLEAN in jsonObject -> BooleanValue.serializer()
            ExtensionType.LONG in jsonObject -> LongValue.serializer()
            ExtensionType.DOUBLE in jsonObject -> DoubleValue.serializer()
            ExtensionType.LIST in jsonObject -> ListValue.serializer()
            ExtensionType.MAP in jsonObject -> MapValue.serializer()
            else -> throw SerializationException("Unknown Extension type. Keys: ${jsonObject.keys}")
        }
    }
}
