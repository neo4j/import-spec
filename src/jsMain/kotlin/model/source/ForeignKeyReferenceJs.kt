package model.source

import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface ForeignKeyReferenceJs {
    val table: String
    val fields: Array<String>
}

fun foreignKeyReferenceJs(
    table: String,
    fields: Array<String>,
) = object : ForeignKeyReferenceJs {
    override val table = table
    override val fields = fields
}

fun ForeignKeyReference.toJs() = foreignKeyReferenceJs(
    table = table,
    fields = fields.toTypedArray(),
)

fun ForeignKeyReferenceJs.toClass(): ForeignKeyReference {
    return ForeignKeyReference(
        table = table,
        fields = fields.toSet(),
    )
}
