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
package model.node

import model.dropAt
import model.extension.ExtensionValueJs
import model.remove
import kotlin.collections.indexOf
import kotlin.collections.plus

@JsExport
class NodeIndexEditor {
    companion object {
        @JsStatic
        fun setType(index: NodeIndexJs, type: String) {
            index.type = type
        }

        @JsStatic
        fun addLabel(index: NodeIndexJs, label: String) {
            if (!index.labels.contains(label)) {
                index.labels += label
            }
        }

        @JsStatic
        fun removeLabel(index: NodeIndexJs, label: String) {
            val idx = index.labels.indexOf(label)
            if (idx != -1) {
                index.labels = index.labels.dropAt(idx)
            }
        }

        @JsStatic
        fun addProperty(index: NodeIndexJs, property: String) {
            if (!index.properties.contains(property)) {
                index.properties += property
            }
        }

        @JsStatic
        fun removeProperty(index: NodeIndexJs, property: String) {
            val idx = index.properties.indexOf(property)
            if (idx != -1) {
                index.properties = index.properties.dropAt(idx)
            }
        }

        @JsStatic
        fun setOption(index: NodeIndexJs, key: String, value: ExtensionValueJs) {
            index.options[key] = value
        }

        @JsStatic
        fun removeOption(index: NodeIndexJs, key: String) {
            index.options.remove(key)
        }
    }
}
