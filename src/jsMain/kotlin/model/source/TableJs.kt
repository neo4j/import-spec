package model.source

import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.associateBy
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface TableJs {
    val source: String
    val fields: ReadonlyRecord<String, TableFieldJs>
    val primaryKeys: Array<String>
    val foreignKeys: ReadonlyRecord<String, ForeignKeyJs>
}

fun tableJs(
    source: String,
    fields: ReadonlyRecord<String, TableFieldJs>,
    primaryKeys: Array<String>,
    foreignKeys: ReadonlyRecord<String, ForeignKeyJs>,
) = object : TableJs {
    override val source = source
    override val fields = fields
    override val primaryKeys = primaryKeys
    override val foreignKeys = foreignKeys
}

fun Table.toJs() = tableJs(
    source = source,
    fields = fields.map { (key, value) -> key to value.toJs() }.toMap().toReadonlyRecord(),
    primaryKeys = primaryKeys.toTypedArray(),
    foreignKeys = foreignKeys.map { it.key to it.value.toJs() }.toMap().toReadonlyRecord(),
)

fun TableJs.toClass(): Table {
    return Table(
        source = source,
        fields = fields.associateBy { _, field -> field.toClass() },
        primaryKeys = primaryKeys.toSet(),
        foreignKeys = foreignKeys.associateBy { _, fk -> fk.toClass() },
    )
}
