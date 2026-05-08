package model.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForeignKeyEditorTest {

    @Test
    fun testAddField_whenNew_addsField() {
        val fk = foreignKeyJs(arrayOf("user_id"), foreignKeyReferenceJs("users"))
        ForeignKeyEditor.addField(fk, "org_id")

        assertEquals(2, fk.fields.size)
        assertTrue(fk.fields.contains("org_id"))
    }

    @Test
    fun testAddField_whenDuplicate_doesNotAdd() {
        val fk = foreignKeyJs(arrayOf("user_id"), foreignKeyReferenceJs("users"))
        ForeignKeyEditor.addField(fk, "user_id")

        assertEquals(1, fk.fields.size)
    }

    @Test
    fun testRemoveField_whenExisting_removesField() {
        val fk = foreignKeyJs(arrayOf("user_id", "org_id"), foreignKeyReferenceJs("users"))
        ForeignKeyEditor.removeField(fk, "user_id")

        assertEquals(1, fk.fields.size)
        assertFalse(fk.fields.contains("user_id"))
    }

    @Test
    fun testRemoveField_whenNonExisting_doesNothing() {
        val fk = foreignKeyJs(arrayOf("user_id"), foreignKeyReferenceJs("users"))
        ForeignKeyEditor.removeField(fk, "non_existent")

        assertEquals(1, fk.fields.size)
    }

    @Test
    fun testSetReferenceTable() {
        val fk = foreignKeyJs(emptyArray(), foreignKeyReferenceJs("users"))
        ForeignKeyEditor.setReferenceTable(fk, "accounts")

        assertEquals("accounts", fk.references.table)
    }

    @Test
    fun testAddReferenceField() {
        val fk = foreignKeyJs(emptyArray(), foreignKeyReferenceJs("users", arrayOf("id")))
        ForeignKeyEditor.addReferenceField(fk, "uuid")

        assertEquals(2, fk.references.fields.size)
        assertTrue(fk.references.fields.contains("uuid"))
    }

    @Test
    fun testRemoveReferenceField() {
        val fk = foreignKeyJs(emptyArray(), foreignKeyReferenceJs("users", arrayOf("id", "uuid")))
        ForeignKeyEditor.removeReferenceField(fk, "id")

        assertEquals(1, fk.references.fields.size)
        assertFalse(fk.references.fields.contains("id"))
    }
}
