package model.constraint

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeConstraintJs : ConstraintJs {
    val label: String
}
