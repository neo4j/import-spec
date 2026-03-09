package model.source

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class ForeignKey(val field: String, val referencedField: String)
