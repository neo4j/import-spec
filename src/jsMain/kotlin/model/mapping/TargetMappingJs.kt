package model.mapping

import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.associateBy

@JsExport
@JsPlainObject
external interface TargetMappingJs {
    val node: String
    val properties: ReadonlyRecord<String, PropertyMappingJs>
}

fun targetMappingJs(node: String, properties: ReadonlyRecord<String, PropertyMappingJs>) = object : TargetMappingJs {
    override val node = node
    override val properties = properties
}

fun TargetMapping.toJs() = targetMappingJs(
    node = node,
    properties = properties.map { it.key to it.value.toJs() }.toMap().toReadonlyRecord(),
)

fun TargetMappingJs.toClass(): TargetMapping {
    return TargetMapping(
        node = node,
        properties = properties.associateBy { _, value -> value.toClass() }
    )
}
