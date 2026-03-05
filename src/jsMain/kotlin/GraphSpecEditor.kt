@file:OptIn(ExperimentalJsStatic::class)
import js.objects.emptyReadonlyRecord
import js.objects.emptyRecord
import model.GraphModel
import model.GraphModelJs
import model.Node
import model.RelationshipJs

@JsExport
class GraphSpecEditor {
    companion object {
        @JsStatic
        fun plain(model: GraphModel): GraphModelJs {
            return GraphModelJs(model.version, emptyReadonlyRecord<String, RelationshipJs>(), emptyReadonlyRecord<String, RelationshipJs>())
        }

        @JsStatic
        fun model(model: GraphModelJs): GraphModel {
            return GraphModel(model.version)
        }

        fun addNode(nodes: Map<String, Node>): Map<String, Node> {
            return nodes
        }
    }
}
