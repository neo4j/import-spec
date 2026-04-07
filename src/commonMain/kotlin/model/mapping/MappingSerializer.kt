package model.mapping

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object MappingSerializer : JsonContentPolymorphicSerializer<Mapping>(Mapping::class) {
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
