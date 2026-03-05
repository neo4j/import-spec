package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipPointIndexJs : RelationshipIndexJs {
    val options: Map<String, Any>
}
