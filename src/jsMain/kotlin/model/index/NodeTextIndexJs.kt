package model.index

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodeTextIndexJs : NodeIndexJs {
    val options: ReadonlyRecord<String, Any>
}
