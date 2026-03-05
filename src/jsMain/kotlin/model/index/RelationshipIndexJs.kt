package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipIndexJs : IndexJs {
    val properties: Array<String>
}
