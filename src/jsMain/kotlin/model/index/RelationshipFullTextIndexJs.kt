package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipFullTextIndexJs : RelationshipIndexJs {
    val options: Map<String, Any>
}
