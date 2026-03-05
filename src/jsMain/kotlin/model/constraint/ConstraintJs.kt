package model.constraint

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface ConstraintJs {
    val type: String
    val properties: Array<String>
}
