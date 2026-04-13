package model.mapping

import model.JsMappingTest
import model.Labels
import model.LabelsJs
import model.constraint.NodeConstraint
import model.constraint.NodeConstraintJs
import model.constraint.toClass
import model.constraint.toJs
import model.extension.StringValue
import model.extension.stringValueJs
import model.index.RelationshipIndex
import model.index.RelationshipIndexJs
import model.index.toClass
import model.index.toJs
import model.toClass
import model.toJs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryMappingJsTest : JsMappingTest<QueryMapping, QueryMappingJs>() {

    override fun createClass() = QueryMapping(
        table = "table_name",
        query = "cypher query",
    )

    override fun toJs(k: QueryMapping): QueryMappingJs = k.toJs()

    override fun toClass(js: QueryMappingJs): QueryMapping = js.toClass()

    override fun verifyJsObject(jsObject: QueryMappingJs) {
        assertEquals("QueryMapping", jsObject.type)
        assertEquals("table_name", jsObject.table)
        assertEquals("cypher query", jsObject.query)
    }

}
