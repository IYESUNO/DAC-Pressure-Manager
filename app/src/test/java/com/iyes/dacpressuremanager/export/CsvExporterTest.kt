package com.iyes.dacpressuremanager.export

import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    private val exporter = CsvExporter(
        now = { 9_999L },
        recordTimestampFormatter = { "T$it" },
        fileTimestampFormatter = { "STAMP" },
    )

    @Test
    fun csvUsesBomCrLfNineColumnsAndChronologicalOrder() {
        val profile = profile(name = """=Lab, "A"/B""")
        val records = listOf(
            record(id = 2, timestamp = 2_000, pressureCenti = 3_000),
            record(id = 1, timestamp = 1_000, pressureCenti = 2_939),
        )

        val document = exporter.build(profile, records)
        val text = document.bytes.toString(StandardCharsets.UTF_8)

        assertArrayEquals(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
            document.bytes.copyOfRange(0, 3),
        )
        assertTrue(text.startsWith("\uFEFF"))
        assertEquals(2, Regex("\r\n").findAll(text).count())
        assertFalse(text.replace("\r\n", "").contains('\n'))
        assertEquals(9, text.lineSequence().first().split(',').size)
        assertEquals(
            "\uFEFFNo.,Timestamp,Mode,Profile,Reference,Measured,Shift,Input Unit,Pressure (GPa)\r\n" +
                "1,T1000,Diamond,\"'=Lab, \"\"A\"\"/B\",1333.00,1400.00,67.00,cm⁻¹,29.39\r\n" +
                "2,T2000,Diamond,\"'=Lab, \"\"A\"\"/B\",1333.00,1400.00,67.00,cm⁻¹,30.00",
            text,
        )
        assertEquals(
            "Diamond_=Lab,--A--B_History_STAMP.csv",
            document.fileName,
        )
    }

    @Test
    fun csvEscapingProtectsSpreadsheetFormulaPrefixes() {
        assertEquals("'=SUM(A1)", exporter.escape("=SUM(A1)", protectFormula = true))
        assertEquals("'+2", exporter.escape("+2", protectFormula = true))
        assertEquals("'-2", exporter.escape("-2", protectFormula = true))
        assertEquals("'@name", exporter.escape("@name", protectFormula = true))
        assertEquals("'\tcell", exporter.escape("\tcell", protectFormula = true))
        assertEquals("\"'\rcell\"", exporter.escape("\rcell", protectFormula = true))
        assertEquals("\"a,b\"", exporter.escape("a,b"))
        assertEquals("\"a\"\"b\"", exporter.escape("a\"b"))
    }

    @Test
    fun rubyCsvAddsTemperatureOnlyWhenHistoryContainsNonRoomRecord() {
        val rubyProfile = Profile(
            id = 20,
            mode = PressureMode.RUBY,
            name = "Ruby #1",
            referenceCenti = 69_424,
            measuredCenti = 69_524,
            sortOrder = 0,
        )
        val roomRecord = HistoryRecord(
            id = 1,
            profileId = 20,
            createdAtEpochMillis = 1_000,
            referenceCenti = 69_424,
            measuredCenti = 69_524,
            pressureCenti = 276,
        )
        val roomText = exporter.build(rubyProfile, listOf(roomRecord))
            .bytes.toString(StandardCharsets.UTF_8)
        assertFalse(roomText.contains("Temperature (K)"))

        val correctedRecord = roomRecord.copy(
            id = 2,
            createdAtEpochMillis = 2_000,
            pressureCenti = 308,
            temperatureK = 278,
        )
        val correctedText = exporter.build(rubyProfile, listOf(roomRecord, correctedRecord))
            .bytes.toString(StandardCharsets.UTF_8)
        assertTrue(correctedText.contains("Input Unit,Temperature (K),Pressure (GPa)"))
        assertTrue(correctedText.contains("nm,298,2.76"))
        assertTrue(correctedText.contains("nm,278,3.08"))
    }

    @Test
    fun fileNameSanitizationHasSafeFallbackAndLengthLimit() {
        assertEquals("Profile", exporter.sanitizeFileName("... --- "))
        assertEquals(80, exporter.sanitizeFileName("x".repeat(100)).length)
    }

    private fun profile(name: String) = Profile(
        id = 10,
        mode = PressureMode.DIAMOND,
        name = name,
        referenceCenti = 133_300,
        measuredCenti = 140_000,
        sortOrder = 0,
    )

    private fun record(
        id: Long,
        timestamp: Long,
        pressureCenti: Int,
    ) = HistoryRecord(
        id = id,
        profileId = 10,
        createdAtEpochMillis = timestamp,
        referenceCenti = 133_300,
        measuredCenti = 140_000,
        pressureCenti = pressureCenti,
    )
}
