package model.source

import model.addUnique
import model.dropAt
import model.getOrThrow
import model.remove

@JsExport
class TableEditor {
    companion object {
        @JsStatic
        fun setSource(table: TableJs, source: String) {
            table.source = source
        }
        @JsStatic
        fun setFieldType(table: TableJs, fieldId: String, type: String) {
            val field = table.fields.getOrThrow(fieldId, "Field")
            TableFieldEditor.setType(field, type)
        }

        @JsStatic
        fun setFieldSize(table: TableJs, fieldId: String, size: Int) {
            val field = table.fields.getOrThrow(fieldId, "Field")
            TableFieldEditor.setSize(field, size)
        }

        @JsStatic
        fun addField(table: TableJs, type: String): String {
            return table.fields.addUnique("field") {
                tableFieldJs(type)
            }
        }

        @JsStatic
        fun removeField(table: TableJs, fieldId: String) {
            table.fields.remove(fieldId)
        }

        @JsStatic
        fun addPrimaryKey(table: TableJs, key: String) {
            if (!table.primaryKeys.contains(key)) {
                table.primaryKeys += key
            }
        }

        @JsStatic
        fun removePrimaryKey(table: TableJs, key: String) {
            val index = table.primaryKeys.indexOf(key)
            if (index != -1) {
                table.primaryKeys = table.primaryKeys.dropAt(index)
            }
        }

        @JsStatic
        fun addForeignKey(table: TableJs, fields: Array<String>, reference: ForeignKeyReferenceJs): String {
            return table.foreignKeys.addUnique("foreignKey") {
                foreignKeyJs(fields, reference)
            }
        }

        @JsStatic
        fun removeForeignKey(table: TableJs, key: String) {
            table.foreignKeys.remove(key)
        }

    }
}
