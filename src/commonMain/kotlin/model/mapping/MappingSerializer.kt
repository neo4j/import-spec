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

        element("value", buildClassSerialDescriptor("MappingChoices") {
            element(MappingType.NODE, NodeMapping.serializer().descriptor)
            element(MappingType.RELATIONSHIP, RelationshipMapping.serializer().descriptor)
            element(MappingType.QUERY, QueryMapping.serializer().descriptor)
            element(MappingType.LABEL, LabelMapping.serializer().descriptor)
        })
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Mapping> {
        val jsonObject = element.jsonObject
        return when {
            MappingType.NODE in jsonObject -> NodeMapping.serializer()
            MappingType.RELATIONSHIP in jsonObject -> RelationshipMapping.serializer()
            MappingType.QUERY in jsonObject -> QueryMapping.serializer()
            MappingType.LABEL in jsonObject -> LabelMapping.serializer()
            else -> throw SerializationException("Unknown Mapping type. Keys: ${jsonObject.keys}")
        }
    }
}
