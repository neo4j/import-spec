package model

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface PropertyJs {
    val type: String?
    val nullable: Boolean
    val unique: Boolean
}
