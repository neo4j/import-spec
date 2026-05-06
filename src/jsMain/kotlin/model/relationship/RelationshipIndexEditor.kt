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
package model.relationship

import model.dropAt
import model.extension.ExtensionValueJs
import model.remove
import kotlin.collections.plus

@JsExport
class RelationshipIndexEditor {
    companion object {
        @JsStatic
        fun setType(constraint: RelationshipIndexJs, type: String) {
            constraint.type = type
        }

        @JsStatic
        fun addProperty(constraint: RelationshipIndexJs, property: String) {
            if (!constraint.properties.contains(property)) {
                constraint.properties += property
            }
        }

        @JsStatic
        fun removeProperty(constraint: RelationshipIndexJs, property: String) {
            val index = constraint.properties.indexOf(property)
            if (index != -1) {
                constraint.properties = constraint.properties.dropAt(index)
            }
        }

        @JsStatic
        fun setOption(constraint: RelationshipIndexJs, key: String, value: ExtensionValueJs) {
            constraint.options[key] = value
        }

        @JsStatic
        fun removeOption(constraint: RelationshipIndexJs, key: String) {
            constraint.options.remove(key)
        }
    }
}
