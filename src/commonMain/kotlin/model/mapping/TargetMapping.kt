package model.mapping

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class TargetMapping(
    val node: String,
    val properties: Map<String, PropertyMapping> = emptyMap(),
)
