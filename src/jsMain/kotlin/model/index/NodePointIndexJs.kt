package model.index

import js.objects.ReadonlyRecord
import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface NodePointIndexJs : NodeIndexJs {
    val options: ReadonlyRecord<String, Any>
}
