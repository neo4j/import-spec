package model.index

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeIndexJs : IndexJs {
    val labels: Array<String>
    val properties: Array<String>
}
