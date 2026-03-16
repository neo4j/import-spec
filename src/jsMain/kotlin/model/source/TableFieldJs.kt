package model.source

import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface TableFieldJs {
    val type: String
    val size: Int
    val suggested: String?
    val supported: Array<String>
}

fun tableFieldJs(
    type: String,
    size: Int,
    suggested: String?,
    supported: Array<String>,
) = object : TableFieldJs {
    override val type = type
    override val size = size
    override val suggested = suggested
    override val supported = supported
}

fun TableField.toJs() = tableFieldJs(
    type = type,
    size = size,
    suggested = suggested?.name,
    supported = supported.map { it.name }.toTypedArray(),
)

fun TableFieldJs.toClass(): TableField {
    return TableField(
        type = type,
        size = size,
        suggested = suggested?.let { Neo4jType.valueOf(it) },
        supported = supported.map { Neo4jType.valueOf(it) }.toSet(),
    )
}
