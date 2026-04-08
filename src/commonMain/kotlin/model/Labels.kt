package model

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class Labels(
    val identifier: String = "",
    val implied: Set<String> = emptySet(),
    val optional: Set<String> = emptySet(),
)
