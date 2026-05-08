package model.source

import kotlin.test.*

class TableEditorTest {

    @Test
    fun testSetSource() {
        val table = tableJs(source = "public.users")
        TableEditor.setSource(table, "private.users")
        assertEquals("private.users", table.source)
    }

    @Test
    fun testAddAndRemoveField() {
        val table = tableJs(source = "users")

        // Test Add
        val fieldId = TableEditor.addField(table, "STRING")
        assertTrue(fieldId.isNotEmpty(), "Add field should return an ID.")

        // Assert field exists using dynamic access for JS plain object
        val addedField = table.fields[fieldId]
        assertNotNull(addedField, "Field should exist in Record.")
        assertEquals("STRING", addedField.type)

        // Test Remove
        TableEditor.removeField(table, fieldId)
        val removedField = table.fields[fieldId]
        assertNull(removedField, "Field should be removed from Record.")
    }

    @Test
    fun testSetFieldType() {
        val table = tableJs(source = "users")
        val fieldId = TableEditor.addField(table, "STRING")

        TableEditor.setFieldType(table, fieldId, "INTEGER")

        val field = table.fields[fieldId]
        assertEquals("INTEGER", field?.type)
    }

    @Test
    fun testSetFieldSize() {
        val table = tableJs(source = "users")
        val fieldId = TableEditor.addField(table, "STRING")

        TableEditor.setFieldSize(table, fieldId, 128)

        val field = table.fields[fieldId]
        assertEquals(128, field?.size)
    }

    @Test
    fun testAddPrimaryKey_whenNew_addsKey() {
        val table = tableJs("users")
        TableEditor.addPrimaryKey(table, "id")

        assertEquals(1, table.primaryKeys.size)
        assertTrue(table.primaryKeys.contains("id"))
    }

    @Test
    fun testAddPrimaryKey_whenDuplicate_doesNotAdd() {
        val table = tableJs("users", primaryKeys = arrayOf("id"))
        TableEditor.addPrimaryKey(table, "id")

        assertEquals(1, table.primaryKeys.size)
    }

    @Test
    fun testRemovePrimaryKey_whenExisting_removesKey() {
        val table = tableJs("users", primaryKeys = arrayOf("id", "uuid"))
        TableEditor.removePrimaryKey(table, "id")

        assertEquals(1, table.primaryKeys.size)
        assertFalse(table.primaryKeys.contains("id"))
    }

    @Test
    fun testRemovePrimaryKey_whenNonExisting_doesNothing() {
        val table = tableJs("users", primaryKeys = arrayOf("id"))
        TableEditor.removePrimaryKey(table, "uuid")

        assertEquals(1, table.primaryKeys.size)
    }

    @Test
    fun testAddAndRemoveForeignKey() {
        val table = tableJs("users")
        val ref = foreignKeyReferenceJs("orgs", arrayOf("org_id"))

        // Test Add
        val fkId = TableEditor.addForeignKey(table, arrayOf("org_id"), ref)
        assertTrue(fkId.isNotEmpty())

        val addedFk = table.foreignKeys[fkId]
        assertNotNull(addedFk, "Foreign key should exist in Record.")

        // Test Remove
        TableEditor.removeForeignKey(table, fkId)
        val removedFk = table.foreignKeys[fkId]
        assertNull(removedFk, "Foreign key should be removed from Record.")
    }
}
