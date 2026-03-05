package model.index

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeVectorIndexJs : NodeIndexJs {
    val options: ReadonlyRecord<String, Any>
}
