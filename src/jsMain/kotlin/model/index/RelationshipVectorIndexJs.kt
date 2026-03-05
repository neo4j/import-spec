package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipVectorIndexJs : RelationshipIndexJs {
    val options: Map<String, Any>
}
