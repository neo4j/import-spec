package model.source

import kotlinx.js.JsPlainObject
import model.Neo4jType
import model.mapping.PropertyMapping
import kotlin.String

@JsExport
@JsPlainObject
external interface ForeignKeyJs {
    val fields: Array<String>
    val references: ForeignKeyReferenceJs
}

fun foreignKeyJs(
    fields: Array<String>,
    references: ForeignKeyReferenceJs,
) = object : ForeignKeyJs {
    override val fields = fields
    override val references = references
}

fun ForeignKey.toJs() = foreignKeyJs(
    fields = fields.toTypedArray(),
    references = references.toJs(),
)

fun ForeignKeyJs.toClass(): ForeignKey {
    return ForeignKey(
        fields = fields.toSet(),
        references = references.toClass(),
    )
}
