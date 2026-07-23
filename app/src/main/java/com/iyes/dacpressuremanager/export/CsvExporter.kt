package com.iyes.dacpressuremanager.export

import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.domain.formatCenti
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CsvDocument(
    val fileName: String,
    val bytes: ByteArray,
)

class CsvExporter(
    private val now: () -> Long = System::currentTimeMillis,
    private val recordTimestampFormatter: (Long) -> String = { epochMillis ->
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.MEDIUM,
            Locale.getDefault(),
        ).format(Date(epochMillis))
    },
    private val fileTimestampFormatter: (Long) -> String = { epochMillis ->
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(epochMillis))
    },
) {
    fun build(
        profile: Profile,
        records: List<HistoryRecord>,
    ): CsvDocument {
        val modeLabel = if (profile.mode == PressureMode.DIAMOND) "Diamond" else "Ruby"
        val headers = listOf(
            "No.",
            "Timestamp",
            "Mode",
            "Profile",
            "Reference",
            "Measured",
            "Shift",
            "Input Unit",
            "Pressure (GPa)",
        )
        val chronologicalRecords = records.sortedWith(
            compareBy<HistoryRecord> { it.createdAtEpochMillis }.thenBy { it.id },
        )
        val rows = chronologicalRecords.mapIndexed { index, record ->
            listOf(
                escape(index + 1),
                escape(recordTimestampFormatter(record.createdAtEpochMillis), protectFormula = true),
                escape(modeLabel, protectFormula = true),
                escape(profile.name, protectFormula = true),
                escape(formatCenti(record.referenceCenti)),
                escape(formatCenti(record.measuredCenti)),
                escape(formatCenti(record.measuredCenti - record.referenceCenti)),
                escape(profile.mode.unit, protectFormula = true),
                escape(formatCenti(record.pressureCenti)),
            ).joinToString(",")
        }
        val csv = buildString {
            append(headers.joinToString(",") { escape(it) })
            rows.forEach { row ->
                append("\r\n")
                append(row)
            }
        }
        val safeProfileName = sanitizeFileName(profile.name)
        val fileName = "${modeLabel}_${safeProfileName}_History_${fileTimestampFormatter(now())}.csv"
        return CsvDocument(
            fileName = fileName,
            bytes = ("\uFEFF$csv").toByteArray(StandardCharsets.UTF_8),
        )
    }

    internal fun escape(
        value: Any?,
        protectFormula: Boolean = false,
    ): String {
        var text = value?.toString().orEmpty()
        if (protectFormula && text.firstOrNull() in FORMULA_PREFIXES) {
            text = "'$text"
        }
        return if (text.any { it == '"' || it == ',' || it == '\r' || it == '\n' }) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    internal fun sanitizeFileName(name: String): String {
        val replaced = name
            .replace(INVALID_FILE_NAME_CHARACTERS, "-")
            .replace(WHITESPACE, "-")
            .trimEnd('.', ' ', '-')
            .take(80)
        return replaced.ifEmpty { "Profile" }
    }

    private companion object {
        val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')
        val INVALID_FILE_NAME_CHARACTERS =
            Regex("""[<>:"/\\|?*\u0000-\u001F]""")
        val WHITESPACE = Regex("""\s+""")
    }
}

