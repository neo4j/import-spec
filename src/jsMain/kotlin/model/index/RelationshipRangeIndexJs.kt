package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipRangeIndexJs : RelationshipIndexJs {
    val options: Map<String, Any>
}
