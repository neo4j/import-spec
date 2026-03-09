package model.source

import kotlinx.serialization.Serializable
import model.Neo4jType
import kotlin.js.JsExport

@JsExport
@Serializable
data class TableField(
    val name: String,
    val type: String,
    val size: Int = 0, // TODO: Is size needed or used?
    val suggested: Neo4jType = Neo4jType.STRING,
    val supported: Set<Neo4jType> = emptySet(),
)
