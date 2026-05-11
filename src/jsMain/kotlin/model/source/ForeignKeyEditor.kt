/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
