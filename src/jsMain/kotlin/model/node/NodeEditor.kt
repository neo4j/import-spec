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

import js.objects.Object
import js.objects.Record
import model.GraphModelJs
import model.addUnique
import model.emptyRecord
import model.extension.ExtensionValueJs
import model.getOrThrow
import model.property.PropertyEditor
import model.property.PropertyJs
import model.property.propertyJs
import model.remove

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
            return node.properties.addUnique("property") { id ->
                propertyJs(id = id, name = id)
            }
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

        @JsStatic
        fun addConstraint(
            model: GraphModelJs,
            nodeId: String,
            type: String,
            label: String? = null,
            properties: Array<String> = emptyArray(),
            extensions: Record<String, ExtensionValueJs> = emptyRecord()
        ): String {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            return node.constraints.addUnique("constraint") {
                nodeConstraintJs(type, label, properties, extensions)
            }
        }

        @JsStatic
        fun setConstraintType(model: GraphModelJs, nodeId: String, constraintId: String, type: String) {
            val constraint = getConstraint(model, nodeId, constraintId)
            NodeConstraintEditor.setType(constraint, type)
        }

        @JsStatic
        fun setConstraintLabel(model: GraphModelJs, nodeId: String, constraintId: String, label: String) {
            val constraint = getConstraint(model, nodeId, constraintId)
            NodeConstraintEditor.setLabel(constraint, label)
        }

        @JsStatic
        fun addConstraintProperty(model: GraphModelJs, nodeId: String, constraintId: String, propertyId: String) {
            val constraint = getConstraint(model, nodeId, constraintId)
            NodeConstraintEditor.addProperty(constraint, propertyId)
        }

        @JsStatic
        fun removeConstraintProperty(model: GraphModelJs, nodeId: String, constraintId: String, propertyId: String) {
            val constraint = getConstraint(model, nodeId, constraintId)
            NodeConstraintEditor.removeProperty(constraint, propertyId)
        }

        private fun getConstraint(model: GraphModelJs, nodeId: String, constraintId: String): NodeConstraintJs {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            val constraint = node.constraints.getOrThrow(constraintId, "Constraint")
            return constraint
        }

        /*
            Indexes
         */

        @JsStatic
        fun addIndex(
            model: GraphModelJs,
            nodeId: String,
            type: String,
            labels: Array<String> = emptyArray(),
            properties: Array<String> = emptyArray(),
            options: Record<String, ExtensionValueJs> = emptyRecord(),
            extensions: Record<String, ExtensionValueJs> = emptyRecord()
        ): String {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            return node.indexes.addUnique("index") {
                nodeIndexJs(type, labels, properties, options, extensions)
            }
        }

        @JsStatic
        fun setIndexType(model: GraphModelJs, nodeId: String, indexId: String, type: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.setType(index, type)
        }

        @JsStatic
        fun addIndexLabel(model: GraphModelJs, nodeId: String, indexId: String, label: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.addLabel(index, label)
        }

        @JsStatic
        fun removeIndexLabel(model: GraphModelJs, nodeId: String, indexId: String, label: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.removeLabel(index, label)
        }

        @JsStatic
        fun addIndexProperty(model: GraphModelJs, nodeId: String, indexId: String, propertyId: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.addProperty(index, propertyId)
        }

        @JsStatic
        fun removeIndexProperty(model: GraphModelJs, nodeId: String, indexId: String, propertyId: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.removeProperty(index, propertyId)
        }

        @JsStatic
        fun setIndexOption(model: GraphModelJs, nodeId: String, indexId: String, key: String, value: ExtensionValueJs) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.setOption(index, key, value)
        }

        @JsStatic
        fun removeIndexOption(model: GraphModelJs, nodeId: String, indexId: String, key: String) {
            val index = getIndex(model, nodeId, indexId)
            NodeIndexEditor.removeOption(index, key)
        }

        private fun getIndex(model: GraphModelJs, nodeId: String, indexId: String): NodeIndexJs {
            val node = model.nodes.getOrThrow(nodeId, "Node")
            val index = node.indexes.getOrThrow(indexId, "Index")
            return index
        }
    }
}
