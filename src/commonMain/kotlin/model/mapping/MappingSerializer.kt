package model.mapping

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object MappingSerializer : JsonContentPolymorphicSerializer<Mapping>(Mapping::class) {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Mapping", SerialKind.CONTEXTUAL)
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
