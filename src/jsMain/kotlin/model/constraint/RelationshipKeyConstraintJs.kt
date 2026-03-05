package model.constraint

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface RelationshipKeyConstraintJs : NodeConstraintJs {
    val options: ReadonlyRecord<String, Any>
}
