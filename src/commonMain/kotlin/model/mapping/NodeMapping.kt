package model.mapping

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class NodeMapping(
    val node: String,
    val source: String, // TODO do sources belong in mapping or tables?
    val table: String, // TODO are these needed?
    val mode: String,
    val matchLabel: String,
    val keys: Set<String> = emptySet(),
    val properties: Map<String, PropertyMapping> = emptyMap()
) : Mapping()
