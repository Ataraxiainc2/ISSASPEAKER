package com.issasoda.issaspeaker.data.records

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The feature catalog and the security audit log, as shipped in the APK.
 *
 * Both are tab-separated files from the repository (docs/features/catalog.tsv and
 * docs/security/findings.tsv), copied into the assets at build time by the copyProjectRecords task
 * in app/build.gradle.kts. The repository is the single source; the app only reads.
 */
data class ProjectRecords(val features: List<Feature>, val findings: List<Finding>) {

    fun count(status: FeatureStatus): Int = features.count { it.status == status }

    companion object {
        const val CATALOG_ASSET = "catalog.tsv"
        const val FINDINGS_ASSET = "findings.tsv"

        suspend fun load(context: Context): ProjectRecords = withContext(Dispatchers.IO) {
            fun read(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
            ProjectRecords(
                features = Feature.parseCatalog(read(CATALOG_ASSET)),
                findings = Finding.parseFindings(read(FINDINGS_ASSET)),
            )
        }
    }
}

enum class FeatureStatus(val key: String) {
    IMPLEMENTED("implemented"),
    PLANNED("planned"),
    VALIDATE("validate"),
    BLOCKED("blocked"),
    ;

    val isPending: Boolean get() = this == VALIDATE || this == BLOCKED

    companion object {
        fun of(key: String): FeatureStatus =
            entries.firstOrNull { it.key == key } ?: throw IllegalArgumentException("unknown status '$key'")
    }
}

data class Feature(
    val id: String,
    val module: String,
    val priority: String,
    val milestone: String,
    val origin: String,
    val status: FeatureStatus,
    val titleEn: String,
    val titleEs: String,
    val note: String,
) {
    /** The catalog carries English and Spanish titles; every other language reads the English one. */
    fun title(language: String): String = if (language == "es") titleEs else titleEn

    companion object {
        val CATALOG_COLUMNS = listOf("id", "module", "priority", "milestone", "origin", "status", "title_en", "title_es", "note")

        fun parseCatalog(text: String): List<Feature> = parseTsv(text, CATALOG_COLUMNS).map { cells ->
            Feature(
                id = cells[0],
                module = cells[1],
                priority = cells[2],
                milestone = cells[3],
                origin = cells[4],
                status = FeatureStatus.of(cells[5]),
                titleEn = cells[6],
                titleEs = cells[7],
                note = cells[8],
            )
        }
    }
}

/** One entry of the security audit log; the ID (ISP-SEC-NNN) is the same in docs/security/AUDIT-LOG.md. */
data class Finding(
    val id: String,
    val date: String,
    val severity: String,
    val status: String,
    val title: String,
    val detail: String,
) {
    companion object {
        val FINDING_COLUMNS = listOf("id", "date", "severity", "status", "title", "detail")

        fun parseFindings(text: String): List<Finding> = parseTsv(text, FINDING_COLUMNS).map { cells ->
            Finding(cells[0], cells[1], cells[2], cells[3], cells[4], cells[5])
        }
    }
}

/** Header must match exactly and every row must have the same number of cells: a broken file fails loudly. */
internal fun parseTsv(text: String, columns: List<String>): List<List<String>> {
    val lines = text.lines().filter { it.isNotBlank() }
    require(lines.isNotEmpty() && lines.first().split('\t') == columns) { "header must be $columns" }
    return lines.drop(1).mapIndexed { index, line ->
        line.split('\t').also { cells ->
            require(cells.size == columns.size) { "row ${index + 2}: ${cells.size} cells, expected ${columns.size}" }
        }
    }
}
