package model.source

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
class Table(
    val source: String,
    val fields: Map<String, TableField> = emptyMap(),
    val primaryKeys: Set<String> = emptySet(),
    val foreignKeys: Map<String, ForeignKey> = emptyMap(),
)
