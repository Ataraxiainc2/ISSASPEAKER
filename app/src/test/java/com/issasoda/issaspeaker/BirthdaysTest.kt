package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.data.birthdays.Birthday
import com.issasoda.issaspeaker.data.birthdays.BirthdayImport
import com.issasoda.issaspeaker.data.birthdays.ImportCandidate
import com.issasoda.issaspeaker.data.birthdays.upcoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BirthdaysTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun birthday(name: String, month: Int, day: Int, year: Int? = null) = Birthday(name, name, month, day, year)

    @Test
    fun `next date, days to go and age`() {
        val ana = birthday("Ana", 10, 15, 1992)
        assertTrue(ana.isToday(today))
        assertEquals(34, ana.turning(today))
        val luis = birthday("Luis", 3, 2, 1980)
        assertEquals(LocalDate.of(2027, 3, 2), luis.next(today))
        assertEquals(47, luis.turning(today))
        assertNull(birthday("Sin año", 12, 1).turning(today))
    }

    @Test
    fun `29 February is celebrated on 28 February in common years`() {
        val leap = birthday("Bisiesto", 2, 29, 2000)
        assertEquals(LocalDate.of(2027, 2, 28), leap.next(today))
        assertEquals(LocalDate.of(2028, 2, 29), leap.next(LocalDate.of(2028, 1, 1)))
        assertTrue(Birthday.isValid(2, 29))
        assertFalse(Birthday.isValid(2, 30))
        assertFalse(Birthday.isValid(13, 1))
    }

    @Test
    fun `upcoming order starts today`() {
        val list = listOf(birthday("Late", 9, 1), birthday("Today", 10, 15), birthday("Soon", 10, 20))
        assertEquals(listOf("Today", "Soon", "Late"), list.upcoming(today).map { it.name })
    }

    @Test
    fun `the owner's sheet shape imports with day first`() {
        val csv = "\"NOMBRE\",\"FN\"\n\"Ana Pérez\",\"25/3/1990\"\n\"Luis\",\"7/11/1985\"\n\"\",\"1/1/2000\"\n\"Sin fecha\",\"\"\n"
        val result = BirthdayImport.parseCsv(csv)
        assertTrue(result.dayFirst)
        assertEquals(listOf(ImportCandidate("Ana Pérez", 3, 25, 1990, ""), ImportCandidate("Luis", 11, 7, 1985, "")), result.candidates)
        assertEquals(2, result.skippedRows)
    }

    @Test
    fun `month first is detected, semicolons and notes are read`() {
        val csv = "Name;Birthday;Notes\r\nJohn;12/25/1970;\"Loves jazz; and \"\"blues\"\"\"\r\nMary;1/2/1999;\r\n"
        val result = BirthdayImport.parseCsv(csv)
        assertFalse(result.dayFirst)
        assertEquals(ImportCandidate("John", 12, 25, 1970, "Loves jazz; and \"blues\""), result.candidates[0])
        assertEquals(ImportCandidate("Mary", 1, 2, 1999, ""), result.candidates[1])
    }

    @Test
    fun `dates as people and contacts write them`() {
        assertEquals(BirthdayImport.ParsedDate(3, 25, 1990), BirthdayImport.parseDate("1990-03-25", dayFirst = true))
        assertEquals(BirthdayImport.ParsedDate(3, 25, null), BirthdayImport.parseDate("--03-25", dayFirst = true))
        assertEquals(BirthdayImport.ParsedDate(3, 25, 1990), BirthdayImport.parseDate("25.03.90", dayFirst = true))
        assertEquals(BirthdayImport.ParsedDate(3, 25, null), BirthdayImport.parseDate("25/03", dayFirst = true))
        assertNull(BirthdayImport.parseDate("31/02/1990", dayFirst = true))
        assertNull(BirthdayImport.parseDate("tomorrow", dayFirst = true))
        assertEquals(ImportCandidate("Ana", 3, 25, null, ""), BirthdayImport.fromContact(" Ana ", "--03-25"))
    }

    @Test
    fun `the same person is recognized across imports`() {
        assertTrue(birthday("Ana  Pérez", 3, 25).sameAs(birthday("ana pérez", 3, 25, 1990)))
        assertFalse(birthday("Ana", 3, 25).sameAs(birthday("Ana", 3, 26)))
    }

    @Test
    fun `a Google Sheet exported as Excel is read, dates as serial numbers or text, other tabs skipped`() {
        val shared = "<sst><si><t>NOMBRE</t></si><si><t>FN</t></si><si><t>Ana &amp; Co</t></si><si><r><t>Lu</t></r><r><t>is</t></r></si></sst>"
        val cumple = "<worksheet><sheetData>" +
            "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c></row>" +
            "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>2</v></c><c r=\"B2\"><v>32957</v></c></row>" +
            "<row r=\"3\"><c r=\"A3\" t=\"s\"><v>3</v></c><c r=\"B3\" t=\"inlineStr\"><is><t>7/11/1985</t></is></c></row>" +
            "</sheetData></worksheet>"
        val facts = "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>A fact</t></is></c></row></sheetData></worksheet>"
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                listOf("xl/sharedStrings.xml" to shared, "xl/worksheets/sheet2.xml" to facts, "xl/worksheets/sheet1.xml" to cumple).forEach { (name, xml) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(xml.toByteArray())
                    zip.closeEntry()
                }
            }
        }.toByteArray()

        val result = BirthdayImport.parseXlsx(bytes)!!

        assertEquals(listOf(ImportCandidate("Ana & Co", 3, 25, 1990, ""), ImportCandidate("Luis", 11, 7, 1985, "")), result.candidates)
        assertNull(BirthdayImport.parseXlsx("not a zip".toByteArray()))
    }
}
