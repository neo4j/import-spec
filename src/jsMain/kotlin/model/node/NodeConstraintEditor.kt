package model.node

import model.dropAt
import kotlin.collections.plus

@JsExport
class NodeConstraintEditor {
    companion object {
        @JsStatic
        fun setType(constraint: NodeConstraintJs, type: String) {
            constraint.type = type
        }

        @JsStatic
        fun setLabel(constraint: NodeConstraintJs, label: String?) {
            constraint.label = label
        }

        @JsStatic
        fun addProperty(constraint: NodeConstraintJs, property: String) {
            if (!constraint.properties.contains(property)) {
                constraint.properties += property
            }
        }

        @JsStatic
        fun removeProperty(constraint: NodeConstraintJs, property: String) {
            val index = constraint.properties.indexOf(property)
            if (index != -1) {
                constraint.properties = constraint.properties.dropAt(index)
            }
        }
    }
}
