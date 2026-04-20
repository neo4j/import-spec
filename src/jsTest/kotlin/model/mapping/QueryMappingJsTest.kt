package model.mapping

import model.mapping.JsMappingTest
import kotlin.test.assertEquals

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
