package model.constraint

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeTypeConstraintJs : NodeConstraintJs {
    val dataType: String
}
