package model.mapping

import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.associateBy

@JsExport
@JsPlainObject
external interface RelationshipMappingJs {
    val relationship: String
    val table: String
    val from: TargetMappingJs
    val to: TargetMappingJs
    val properties: ReadonlyRecord<String, PropertyMappingJs>
    val mode: String
    val matchLabel: String?
    val keys: Array<String>
}

fun relationshipMappingJs(
    relationship: String,
    table: String,
    from: TargetMappingJs,
    to: TargetMappingJs,
    properties: ReadonlyRecord<String, PropertyMappingJs>,
    mode: String,
    matchLabel: String?,
    keys: Array<String>,
) = object : RelationshipMappingJs {
    override val relationship = relationship
    override val table = table
    override val from = from
    override val to = to
    override val properties = properties
    override val mode = mode
    override val matchLabel = matchLabel
    override val keys = keys
}

fun RelationshipMapping.toJs(): RelationshipMappingJs = relationshipMappingJs(
    relationship = relationship,
    table = table,
    from = from.toJs(),
    to = to.toJs(),
    properties = properties.map { it.key to it.value.toJs() }.toMap().toReadonlyRecord(),
    mode = mode.name,
    matchLabel = matchLabel,
    keys = keys.toTypedArray(),
)

fun RelationshipMappingJs.toClass(): RelationshipMapping = RelationshipMapping(
    relationship = relationship,
    table = table,
    from = from.toClass(),
    to = to.toClass(),
    properties = properties.associateBy { _, value -> value.toClass() },
    mode = MappingMode.valueOf(mode),
    matchLabel = matchLabel,
    keys = keys.toSet(),
)
