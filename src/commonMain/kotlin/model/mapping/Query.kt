package model.mapping

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class Query(
    val source: String,
    val table: String,
    val query: String,
) : Mapping()
