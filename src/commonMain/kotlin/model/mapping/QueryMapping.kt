package model.mapping

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
@SerialName(MappingType.QUERY)
data class QueryMapping(val table: String, val query: String) : Mapping
