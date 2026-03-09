package model.source

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class ForeignKeyReference(
    val table: String,
    val fields: Set<String> = emptySet(),
)
