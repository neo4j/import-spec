package model

import js.objects.ReadonlyRecord
import js.objects.toReadonlyRecord
import kotlinx.js.JsPlainObject
import model.constraint.ConstraintJs
import model.constraint.NodeConstraintJs
import model.index.IndexJs
import model.index.NodeIndexJs

@JsExport
@JsPlainObject
external interface NodeJs {
    val labels: Array<String>
    val properties: ReadonlyRecord<String, PropertyJs>
    val constraints: ReadonlyRecord<String, NodeConstraintJs>
    val indexes: ReadonlyRecord<String, NodeIndexJs>
    val extensions: ReadonlyRecord<String, Any>
}
