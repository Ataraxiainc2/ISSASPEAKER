package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.data.records.Feature
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.data.records.Finding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Parses the real files from docs/, the same ones copied into the APK, so a broken edit fails here first. */
class ProjectRecordsTest {

    // Gradle runs unit tests from the module directory (app/).
    private val root = File("..")

    @Test
    fun realCatalogParsesWithSequentialIds() {
        val features = Feature.parseCatalog(root.resolve("docs/features/catalog.tsv").readText())
        assertTrue(features.size >= 400)
        features.forEachIndexed { index, feature ->
            assertEquals("F%03d".format(index + 1), feature.id)
            assertTrue("${feature.id} has titles", feature.titleEn.isNotBlank() && feature.titleEs.isNotBlank())
        }
    }

    @Test
    fun realFindingsFileParses() {
        Finding.parseFindings(root.resolve("docs/security/findings.tsv").readText())
    }

    @Test
    fun titleFollowsLanguage() {
        val feature = Feature.parseCatalog(HEADER + "F001\tPLAT\tP0\tM0\tNEW\tplanned\tHello\tHola\t\n").single()
        assertEquals("Hola", feature.title("es"))
        assertEquals("Hello", feature.title("en"))
        assertEquals("Hello", feature.title("pt"))
        assertEquals(FeatureStatus.PLANNED, feature.status)
        assertEquals("", feature.note)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownStatusFails() {
        Feature.parseCatalog(HEADER + "F001\tPLAT\tP0\tM0\tNEW\tmaybe\tHello\tHola\t\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingCellFails() {
        Feature.parseCatalog(HEADER + "F001\tPLAT\tP0\tM0\tNEW\tplanned\tHello\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongHeaderFails() {
        Feature.parseCatalog("id\tname\nF001\tx\n")
    }

    private companion object {
        val HEADER = Feature.CATALOG_COLUMNS.joinToString("\t") + "\n"
    }
}
