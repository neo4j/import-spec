package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipTextIndexJs : RelationshipIndexJs {
    val options: Map<String, Any>
}
