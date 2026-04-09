package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
@SerialName("Labels")
data class Labels(
    val identifier: String = "",
    val implied: Set<String> = emptySet(),
    val optional: Set<String> = emptySet(),
)
