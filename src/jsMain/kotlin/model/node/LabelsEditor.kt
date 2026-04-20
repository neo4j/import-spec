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

@JsExport
class LabelsEditor {
    companion object {
        @JsStatic
        fun setIdentifier(labels: LabelsJs, identifyingLabel: String) {
            labels.identifier = identifyingLabel
        }

        @JsStatic
        fun addImplied(labels: LabelsJs, label: String) {
            if (!labels.implied.contains(label)) {
                labels.implied += label
            }
        }

        @JsStatic
        fun removeImplied(labels: LabelsJs, label: String) {
            val index = labels.implied.indexOf(label)
            if (index != -1) {
                labels.implied = labels.implied.dropAt(index)
            }
        }

        @JsStatic
        fun addOptional(labels: LabelsJs, label: String) {
            if (!labels.optional.contains(label)) {
                labels.optional += label
            }
        }

        @JsStatic
        fun removeOptional(labels: LabelsJs, label: String) {
            val index = labels.optional.indexOf(label)
            if (index != -1) {
                labels.optional = labels.optional.dropAt(index)
            }
        }
    }
}
