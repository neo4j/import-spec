package model.mapping

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface PropertyMappingJs {
    val field: String
}

fun propertyMappingJs(field: String) = object : PropertyMappingJs {
    override val field = field
}

fun PropertyMapping.toJs() = propertyMappingJs(field)

fun PropertyMappingJs.toClass(): PropertyMapping {
    return PropertyMapping(field)
}
