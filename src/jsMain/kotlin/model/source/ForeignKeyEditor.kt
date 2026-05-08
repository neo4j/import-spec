package model.source

import model.dropAt
import kotlin.collections.plus

@JsExport
class ForeignKeyEditor {
    companion object {
        @JsStatic
        fun addField(foreignKey: ForeignKeyJs, field: String) {
            if (!foreignKey.fields.contains(field)) {
                foreignKey.fields += field
            }
        }

        @JsStatic
        fun removeField(foreignKey: ForeignKeyJs, field: String) {
            val index = foreignKey.fields.indexOf(field)
            if (index != -1) {
                foreignKey.fields = foreignKey.fields.dropAt(index)
            }
        }

        @JsStatic
        fun setReferenceTable(foreignKey: ForeignKeyJs, table: String) {
            ForeignKeyReferenceEditor.setTable(foreignKey.references, table)
        }

        @JsStatic
        fun addReferenceField(foreignKey: ForeignKeyJs, field: String) {
            ForeignKeyReferenceEditor.addField(foreignKey.references, field)
        }

        @JsStatic
        fun removeReferenceField(foreignKey: ForeignKeyJs, field: String) {
            ForeignKeyReferenceEditor.removeField(foreignKey.references, field)
        }
    }
}
