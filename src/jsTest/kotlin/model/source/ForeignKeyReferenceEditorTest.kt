package model.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForeignKeyReferenceEditorTest {

    @Test
    fun testSetTable() {
        val reference = foreignKeyReferenceJs(table = "users")
        ForeignKeyReferenceEditor.setTable(reference, "accounts")
        assertEquals("accounts", reference.table, "Table reference should be updated.")
    }

    @Test
    fun testAddField_whenNew_addsField() {
        val reference = foreignKeyReferenceJs(table = "users", fields = arrayOf("id"))
        ForeignKeyReferenceEditor.addField(reference, "uuid")

        assertEquals(2, reference.fields.size)
        assertTrue(reference.fields.contains("uuid"), "New field should be added.")
    }

    @Test
    fun testAddField_whenDuplicate_doesNotAdd() {
        val reference = foreignKeyReferenceJs(table = "users", fields = arrayOf("id"))
        ForeignKeyReferenceEditor.addField(reference, "id")

        assertEquals(1, reference.fields.size, "Duplicate field should not be added.")
    }

    @Test
    fun testRemoveField_whenExisting_removesField() {
        val reference = foreignKeyReferenceJs(table = "users", fields = arrayOf("id", "uuid"))
        ForeignKeyReferenceEditor.removeField(reference, "id")

        assertEquals(1, reference.fields.size)
        assertFalse(reference.fields.contains("id"), "Existing field should be removed.")
        assertTrue(reference.fields.contains("uuid"))
    }

    @Test
    fun testRemoveField_whenNonExisting_doesNothing() {
        val reference = foreignKeyReferenceJs(table = "users", fields = arrayOf("id"))
        ForeignKeyReferenceEditor.removeField(reference, "uuid") // Doesn't exist

        assertEquals(1, reference.fields.size, "Array size should remain unchanged.")
        assertTrue(reference.fields.contains("id"))
    }
}
