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
