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
package model

@JsExport
class NodeEditor {
    companion object {
        @JsStatic
        fun setName(model: GraphModelJs, nodeId: String, newName: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            node.name = newName
        }

        /*
            Labels
         */

        @JsStatic
        fun setIdentifyingLabel(model: GraphModelJs, nodeId: String, label: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            LabelsEditor.setIdentifier(node.labels, label)
        }

        @JsStatic
        fun addImpliedLabel(model: GraphModelJs, nodeId: String, label: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            LabelsEditor.addImplied(node.labels, label)
        }

        @JsStatic
        fun removeImpliedLabel(model: GraphModelJs, nodeId: String, label: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            LabelsEditor.removeImplied(node.labels, label)
        }

        @JsStatic
        fun addOptionalLabel(model: GraphModelJs, nodeId: String, label: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            LabelsEditor.addOptional(node.labels, label)
        }

        @JsStatic
        fun removeOptionalLabel(model: GraphModelJs, nodeId: String, label: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            LabelsEditor.removeOptional(node.labels, label)
        }

        /*
            Properties
         */

        @JsStatic
        fun addProperty(model: GraphModelJs, nodeId: String): String {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            val id = node.properties.uniqueKey("property")
            node.properties[id] = propertyJs(id = id, name = id)
            return id
        }

        @JsStatic
        fun removeProperty(model: GraphModelJs, nodeId: String, propertyId: String) {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            node.properties.remove(propertyId)
        }

        @JsStatic
        fun setPropertyName(model: GraphModelJs, nodeId: String, propertyId: String, name: String) {
            val property = getProperty(model, nodeId, propertyId)
            PropertyEditor.setName(property, name)
        }

        @JsStatic
        fun setPropertyType(model: GraphModelJs, nodeId: String, propertyId: String, type: String) { // TODO neo4j type
            val property = getProperty(model, nodeId, propertyId)
            PropertyEditor.setType(property, type)
        }

        @JsStatic
        fun setPropertyNullable(model: GraphModelJs, nodeId: String, propertyId: String, nullable: Boolean) {
            val property = getProperty(model, nodeId, propertyId)
            PropertyEditor.setNullable(property, nullable)
        }

        @JsStatic
        fun setPropertyUnique(model: GraphModelJs, nodeId: String, propertyId: String, unique: Boolean) {
            val property = getProperty(model, nodeId, propertyId)
            PropertyEditor.setUnique(property, unique)
        }

        private fun getProperty(model: GraphModelJs, nodeId: String, propertyId: String): PropertyJs {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            val property = node.properties.getOrThrow(propertyId, "Property")
            return property
        }

        /*
            Constraints
         */

        /*
            Indexes
         */
    }
}
