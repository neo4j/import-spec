package model

import kotlinx.js.JsPlainObject
import model.constraint.ConstraintJs
import model.constraint.RelationshipConstraintJs
import model.index.IndexJs
import model.index.RelationshipIndexJs

@JsExport
@JsPlainObject
external interface RelationshipJs {
    val type: String
    val from: String
    val to: String
    val properties: Map<String, PropertyJs>
    val constraints: Map<String, RelationshipConstraintJs>
    val indexes: Map<String, RelationshipIndexJs>
    val extensions: Map<String, Any>
}
