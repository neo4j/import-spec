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
class RelationshipEditor {
    companion object {
        @JsStatic
        fun setType(relationship: RelationshipJs, type: String) {
            relationship.type = type
        }

        @JsStatic
        fun setName(relationship: RelationshipJs, name: String) {
            relationship.name = name
        }

        @JsStatic
        fun setSourceNode(relationship: RelationshipJs, node: String) {
            RelationshipTargetEditor.setNode(relationship.from, node)
        }

        @JsStatic
        fun setSourceLabel(relationship: RelationshipJs, label: String) {
            RelationshipTargetEditor.setLabel(relationship.from, label)
        }

        @JsStatic
        fun setSourceProperty(relationship: RelationshipJs, property: String) {
            RelationshipTargetEditor.setProperty(relationship.from, property)
        }

        @JsStatic
        fun setTargetNode(relationship: RelationshipJs, node: String) {
            RelationshipTargetEditor.setNode(relationship.to, node)
        }

        @JsStatic
        fun setTargetLabel(relationship: RelationshipJs, label: String) {
            RelationshipTargetEditor.setLabel(relationship.to, label)
        }

        @JsStatic
        fun setTargetProperty(relationship: RelationshipJs, property: String) {
            RelationshipTargetEditor.setProperty(relationship.to, property)
        }

        /*
            Properties
         */

        @JsStatic
        fun addProperty(model: GraphModelJs, relationshipId: String): String {
            val relationship = model.relationships.getOrThrow(relationshipId, "Node")
            return relationship.properties.addUnique("property") { id ->
                propertyJs(id = id, name = id)
            }
        }

        @JsStatic
        fun removeProperty(model: GraphModelJs, relationshipId: String, propertyId: String) {
            val relationship = model.relationships.getOrThrow(relationshipId, "Node")
            relationship.properties.remove(propertyId)
        }

        @JsStatic
        fun setPropertyName(model: GraphModelJs, relationshipId: String, propertyId: String, name: String) {
            val property = getProperty(model, relationshipId, propertyId)
            PropertyEditor.setName(property, name)
        }

        // TODO neo4j type
        @JsStatic
        fun setPropertyType(model: GraphModelJs, relationshipId: String, propertyId: String, type: String) {
            val property = getProperty(model, relationshipId, propertyId)
            PropertyEditor.setType(property, type)
        }

        @JsStatic
        fun setPropertyNullable(model: GraphModelJs, relationshipId: String, propertyId: String, nullable: Boolean) {
            val property = getProperty(model, relationshipId, propertyId)
            PropertyEditor.setNullable(property, nullable)
        }

        @JsStatic
        fun setPropertyUnique(model: GraphModelJs, relationshipId: String, propertyId: String, unique: Boolean) {
            val property = getProperty(model, relationshipId, propertyId)
            PropertyEditor.setUnique(property, unique)
        }

        private fun getProperty(model: GraphModelJs, relationshipId: String, propertyId: String): PropertyJs {
            val relationship = model.relationships.getOrThrow(relationshipId, "Relationship")
            val property = relationship.properties.getOrThrow(propertyId, "Property")
            return property
        }

        /*
            Constraints
         */

        @JsStatic
        fun addConstraint(
            model: GraphModelJs,
            relationshipId: String,
            type: String,
            properties: Array<String> = emptyArray(),
            options: Record<String, ExtensionValueJs> = emptyRecord(),
            extensions: Record<String, ExtensionValueJs> = emptyRecord()
        ): String {
            val relationship = model.relationships.getOrThrow(relationshipId, "Relationship")
            return relationship.constraints.addUnique("constraint") {
                relationshipConstraintJs(type, properties, options, extensions)
            }
        }

        @JsStatic
        fun setConstraintType(model: GraphModelJs, relationshipId: String, constraintId: String, type: String) {
            val constraint = getConstraint(model, relationshipId, constraintId)
            RelationshipConstraintEditor.setType(constraint, type)
        }

        @JsStatic
        fun addConstraintProperty(
            model: GraphModelJs,
            relationshipId: String,
            constraintId: String,
            propertyId: String
        ) {
            val constraint = getConstraint(model, relationshipId, constraintId)
            RelationshipConstraintEditor.addProperty(constraint, propertyId)
        }

        @JsStatic
        fun removeConstraintProperty(
            model: GraphModelJs,
            relationshipId: String,
            constraintId: String,
            propertyId: String
        ) {
            val constraint = getConstraint(model, relationshipId, constraintId)
            RelationshipConstraintEditor.removeProperty(constraint, propertyId)
        }

        @JsStatic
        fun setConstraintOption(
            model: GraphModelJs,
            relationshipId: String,
            constraintId: String,
            key: String,
            value: ExtensionValueJs
        ) {
            val constraint = getConstraint(model, relationshipId, constraintId)
            RelationshipConstraintEditor.setOption(constraint, key, value)
        }

        @JsStatic
        fun removeConstraintOption(model: GraphModelJs, relationshipId: String, constraintId: String, key: String) {
            val constraint = getConstraint(model, relationshipId, constraintId)
            RelationshipConstraintEditor.removeOption(constraint, key)
        }

        private fun getConstraint(
            model: GraphModelJs,
            relationshipId: String,
            constraintId: String
        ): RelationshipConstraintJs {
            val relationship = model.relationships.getOrThrow(relationshipId, "Relationship")
            val constraint = relationship.constraints.getOrThrow(constraintId, "Constraint")
            return constraint
        }

        /*
            Indexes
         */

        @JsStatic
        fun addIndex(
            model: GraphModelJs,
            relationshipId: String,
            type: String,
            properties: Array<String> = emptyArray(),
            options: Record<String, ExtensionValueJs> = emptyRecord(),
            extensions: Record<String, ExtensionValueJs> = emptyRecord()
        ): String {
            val relationship = model.relationships.getOrThrow(relationshipId, "Relationship")
            return relationship.indexes.addUnique("index") {
                relationshipIndexJs(type, properties, options, extensions)
            }
        }

        @JsStatic
        fun setIndexType(model: GraphModelJs, relationshipId: String, indexId: String, type: String) {
            val constraint = getIndex(model, relationshipId, indexId)
            RelationshipIndexEditor.setType(constraint, type)
        }

        @JsStatic
        fun addIndexProperty(model: GraphModelJs, relationshipId: String, indexId: String, propertyId: String) {
            val constraint = getIndex(model, relationshipId, indexId)
            RelationshipIndexEditor.addProperty(constraint, propertyId)
        }

        @JsStatic
        fun removeIndexProperty(model: GraphModelJs, relationshipId: String, indexId: String, propertyId: String) {
            val constraint = getIndex(model, relationshipId, indexId)
            RelationshipIndexEditor.removeProperty(constraint, propertyId)
        }

        @JsStatic
        fun setIndexOption(
            model: GraphModelJs,
            relationshipId: String,
            indexId: String,
            key: String,
            value: ExtensionValueJs
        ) {
            val constraint = getIndex(model, relationshipId, indexId)
            RelationshipIndexEditor.setOption(constraint, key, value)
        }

        @JsStatic
        fun removeIndexOption(model: GraphModelJs, relationshipId: String, indexId: String, key: String) {
            val constraint = getIndex(model, relationshipId, indexId)
            RelationshipIndexEditor.removeOption(constraint, key)
        }

        private fun getIndex(model: GraphModelJs, relationshipId: String, indexId: String): RelationshipIndexJs {
            val relationship = model.relationships.getOrThrow(relationshipId, "Relationship")
            val index = relationship.indexes.getOrThrow(indexId, "Index")
            return index
        }
    }
}
