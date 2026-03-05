package model

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface GraphModelJs {
    val version: String
    val nodes: ReadonlyRecord<String, NodeJs>
    val relationships: ReadonlyRecord<String, RelationshipJs>
}
