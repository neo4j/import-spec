package model.source

import kotlin.test.Test
import kotlin.test.assertEquals

class TableFieldEditorTest {

    @Test
    fun testSetType() {
        val field = tableFieldJs(type = "STRING")
        TableFieldEditor.setType(field, "INTEGER")
        assertEquals("INTEGER", field.type, "Field type should be updated.")
    }

    @Test
    fun testSetSize() {
        val field = tableFieldJs(type = "STRING", size = 255)
        TableFieldEditor.setSize(field, 500)
        assertEquals(500, field.size, "Field size should be updated.")
    }
}
