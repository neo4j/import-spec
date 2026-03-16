package model.mapping

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class LabelMapping(
    val table: String,
    val field: String,
) : Mapping
