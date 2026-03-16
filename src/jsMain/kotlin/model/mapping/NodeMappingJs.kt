package model.mapping

import js.collections.ReadonlyMap
import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.associateBy
import model.index.NodeIndex
import model.index.NodeIndexJs
import model.index.nodeIndexJs
import model.index.toClass
import model.toMap

@JsExport
@JsPlainObject
external interface NodeMappingJs {
    val node: String
    val table: String
    val properties: ReadonlyRecord<String, PropertyMappingJs>
    val mode: String
    val matchLabel: String?
    val keys: Array<String>
}

fun nodeMappingJs(
    node: String,
    table: String,
    properties: ReadonlyRecord<String, PropertyMappingJs>,
    mode: String,
    matchLabel: String?,
    keys: Array<String>,
) = object : NodeMappingJs {
    override val node = node
    override val table = table
    override val properties = properties
    override val mode = mode
    override val matchLabel = matchLabel
    override val keys = keys
}

fun NodeMapping.toJs(): NodeMappingJs = nodeMappingJs(
    node = node,
    table = table,
    properties = properties.map { it.key to it.value.toJs() }.toMap().toReadonlyRecord(),
    mode = mode.name,
    matchLabel = matchLabel,
    keys = keys.toTypedArray(),
)

fun NodeMappingJs.toClass(): NodeMapping = NodeMapping(
    node = node,
    table = table,
    properties = properties.associateBy { _, value -> value.toClass() },
    mode = MappingMode.valueOf(mode),
    matchLabel = matchLabel,
    keys = keys.toSet(),
)
