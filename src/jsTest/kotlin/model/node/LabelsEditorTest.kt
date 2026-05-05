package model.node

import kotlin.test.*

class LabelsEditorTest {

    private fun createLabels(): LabelsJs = labelsJs(
        identifier = "Base",
        implied = arrayOf("Entity"),
        optional = arrayOf("Cacheable")
    )

    @Test
    fun testSetIdentifier() {
        val labels = createLabels()
        LabelsEditor.setIdentifier(labels, "NewIdentifier")
        assertEquals("NewIdentifier", labels.identifier)
    }

    @Test
    fun testImpliedLabels() {
        val labels = createLabels()

        // Add new implied label
        LabelsEditor.addImplied(labels, "Persistable")
        assertTrue(labels.implied.contains("Persistable"))
        assertEquals(2, labels.implied.size)

        // Add duplicate implied label
        LabelsEditor.addImplied(labels, "Persistable")
        assertEquals(2, labels.implied.size)

        // Remove implied label
        LabelsEditor.removeImplied(labels, "Entity")
        assertFalse(labels.implied.contains("Entity"))
        assertEquals(1, labels.implied.size)

        // Remove non-existent implied label
        LabelsEditor.removeImplied(labels, "Ghost")
        assertEquals(1, labels.implied.size)
    }

    @Test
    fun testOptionalLabels() {
        val labels = createLabels()

        // Add new optional label
        LabelsEditor.addOptional(labels, "Exportable")
        assertTrue(labels.optional.contains("Exportable"))
        assertEquals(2, labels.optional.size)

        // Add duplicate optional label
        LabelsEditor.addOptional(labels, "Exportable")
        assertEquals(2, labels.optional.size)

        // Remove optional label
        LabelsEditor.removeOptional(labels, "Cacheable")
        assertFalse(labels.optional.contains("Cacheable"))
        assertEquals(1, labels.optional.size)

        // Remove non-existent optional label
        LabelsEditor.removeOptional(labels, "Missing")
        assertEquals(1, labels.optional.size)
    }
}
