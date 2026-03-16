package model.mapping

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
enum class MappingMode {
    MERGE,
    CREATE
}
