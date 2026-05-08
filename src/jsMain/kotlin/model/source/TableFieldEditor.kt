package model.source

@JsExport
class TableFieldEditor {
    companion object {
        @JsStatic
        fun setType(field: TableFieldJs, type: String) {
            field.type = type
        }

        @JsStatic
        fun setSize(field: TableFieldJs, size: Int) {
            field.size = size
        }
    }
}
