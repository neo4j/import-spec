package model.source

import model.dropAt
import kotlin.collections.plus

@JsExport
class ForeignKeyReferenceEditor {
    companion object {
        @JsStatic
        fun setTable(reference: ForeignKeyReferenceJs, table: String) {
            reference.table = table
        }

        @JsStatic
        fun addField(reference: ForeignKeyReferenceJs, field: String) {
            if (!reference.fields.contains(field)) {
                reference.fields += field
            }
        }

        @JsStatic
        fun removeField(reference: ForeignKeyReferenceJs, field: String) {
            val index = reference.fields.indexOf(field)
            if (index != -1) {
                reference.fields = reference.fields.dropAt(index)
            }
        }
    }
}
