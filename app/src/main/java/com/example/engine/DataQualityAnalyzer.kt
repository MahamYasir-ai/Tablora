package com.example.engine

import com.example.model.CleaningOperation
import com.example.model.DataQualityReport
import com.example.model.EmailAction
import com.example.model.PhoneAction
import com.example.model.QualityIssue
import com.example.model.QualityIssueCategory
import com.example.model.QualitySeverity
import com.example.model.SpreadsheetDataset
import java.util.regex.Pattern

/**
 * Computes deterministic, mathematical data quality metrics for any dataset.
 * Does not make arbitrary guesses; clearly distinguishes definite issues from heuristic warnings.
 */
object DataQualityAnalyzer {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )

    private val DATE_PATTERNS = listOf(
        Pattern.compile("""^\d{4}-\d{2}-\d{2}$"""),        // YYYY-MM-DD
        Pattern.compile("""^\d{1,2}/\d{1,2}/\d{4}$"""),    // MM/DD/YYYY or DD/MM/YYYY
        Pattern.compile("""^\d{1,2}-\d{1,2}-\d{4}$"""),    // MM-DD-YYYY
        Pattern.compile("""^[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4}$""") // Month DD, YYYY
    )

    fun analyze(dataset: SpreadsheetDataset): DataQualityReport {
        val totalRows = dataset.rowCount
        val totalCols = dataset.columnCount
        val totalCells = totalRows * totalCols

        if (totalRows == 0 || totalCols == 0) {
            return DataQualityReport(
                qualityScore = 100,
                scoreFormula = "100 - (penalties for missing, duplicate, and format anomalies)",
                totalIssuesCount = 0,
                categories = emptyList(),
                affectedColumnsSummary = emptyMap(),
                emptyRowsCount = 0,
                duplicateRowsCount = 0,
                emptyCellsCount = 0,
                totalCellsCount = 0
            )
        }

        val affectedColsMap = mutableMapOf<String, Int>()
        fun recordAffectedCol(colName: String, count: Int = 1) {
            affectedColsMap[colName] = affectedColsMap.getOrDefault(colName, 0) + count
        }

        // ==========================================
        // 1. MISSING DATA ANALYSIS
        // ==========================================
        var emptyCellsCount = 0
        var whitespaceOnlyCellsCount = 0
        val emptyCols = mutableListOf<String>()
        var emptyRowsCount = 0

        // Check empty rows
        for (row in dataset.rows) {
            if (row.all { it.isBlank() }) emptyRowsCount++
        }

        // Check empty columns & cells
        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            var colEmptyCount = 0
            for (r in 0 until totalRows) {
                val cell = dataset.getCellValue(r, c)
                if (cell.isEmpty()) {
                    emptyCellsCount++
                    colEmptyCount++
                } else if (cell.isNotBlank() && cell.trim().isEmpty()) {
                    // whitespace only
                    emptyCellsCount++
                    whitespaceOnlyCellsCount++
                    colEmptyCount++
                }
            }
            if (colEmptyCount == totalRows) {
                emptyCols.add(colName)
                recordAffectedCol(colName, totalRows)
            } else if (colEmptyCount > 0) {
                recordAffectedCol(colName, colEmptyCount)
            }
        }

        val missingIssues = mutableListOf<QualityIssue>()
        if (emptyRowsCount > 0) {
            missingIssues.add(
                QualityIssue(
                    id = "empty_rows",
                    title = "Fully Empty Rows",
                    description = "$emptyRowsCount row(s) contain only empty or whitespace cells.",
                    count = emptyRowsCount,
                    severity = QualitySeverity.ERROR,
                    affectedColumns = emptyList(),
                    suggestedOperation = CleaningOperation.RemoveEmptyRows
                )
            )
        }
        if (emptyCols.isNotEmpty()) {
            missingIssues.add(
                QualityIssue(
                    id = "empty_cols",
                    title = "Fully Empty Columns",
                    description = "${emptyCols.size} column(s) have no data in any row.",
                    count = emptyCols.size,
                    severity = QualitySeverity.ERROR,
                    affectedColumns = emptyCols,
                    suggestedOperation = CleaningOperation.RemoveEmptyColumns
                )
            )
        }
        if (emptyCellsCount > 0) {
            missingIssues.add(
                QualityIssue(
                    id = "blank_cells",
                    title = "Missing or Blank Cells",
                    description = "$emptyCellsCount individual cell(s) are blank or null across the dataset.",
                    count = emptyCellsCount,
                    severity = QualitySeverity.WARNING,
                    affectedColumns = affectedColsMap.keys.toList()
                )
            )
        }

        // ==========================================
        // 2. DUPLICATES ANALYSIS
        // ==========================================
        val seenRows = mutableSetOf<String>()
        var duplicateRowsCount = 0
        for (row in dataset.rows) {
            val rowFingerprint = row.joinToString("\u0000")
            if (!seenRows.add(rowFingerprint)) {
                duplicateRowsCount++
            }
        }

        val duplicatesIssues = mutableListOf<QualityIssue>()
        if (duplicateRowsCount > 0) {
            duplicatesIssues.add(
                QualityIssue(
                    id = "duplicate_rows",
                    title = "Exact Duplicate Rows",
                    description = "$duplicateRowsCount row(s) are exact duplicates of earlier rows.",
                    count = duplicateRowsCount,
                    severity = QualitySeverity.ERROR,
                    affectedColumns = dataset.columns,
                    suggestedOperation = CleaningOperation.RemoveExactDuplicates
                )
            )
        }

        // Heuristic: check duplicates in ID / Email columns
        for (col in dataset.columns) {
            val lower = col.lowercase()
            if (lower.contains("email") || lower.contains("id") || lower.contains("sku")) {
                val colIdx = dataset.columns.indexOf(col)
                val vals = dataset.rows.mapNotNull {
                    val v = it.getOrNull(colIdx)?.trim()
                    if (v.isNullOrEmpty()) null else v
                }
                val uniqueCount = vals.toSet().size
                val colDuplicates = vals.size - uniqueCount
                if (colDuplicates > 0) {
                    duplicatesIssues.add(
                        QualityIssue(
                            id = "duplicate_id_$col",
                            title = "Duplicate Values in Key Column ($col)",
                            description = "$colDuplicates duplicate entry/entries found in '$col'.",
                            count = colDuplicates,
                            severity = QualitySeverity.WARNING,
                            affectedColumns = listOf(col),
                            suggestedOperation = CleaningOperation.RemoveDuplicatesByColumns(listOf(col))
                        )
                    )
                    recordAffectedCol(col, colDuplicates)
                }
            }
        }

        // ==========================================
        // 3. TEXT QUALITY ANALYSIS
        // ==========================================
        var unpaddedWhitespaceCount = 0
        var multiSpaceCount = 0
        val whitespaceCols = mutableSetOf<String>()
        val multiSpaceCols = mutableSetOf<String>()
        val unpaddedSamples = mutableListOf<String>()

        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            for (r in 0 until totalRows) {
                val cell = dataset.getCellValue(r, c)
                if (cell.isNotEmpty()) {
                    if (cell.startsWith(" ") || cell.endsWith(" ")) {
                        unpaddedWhitespaceCount++
                        whitespaceCols.add(colName)
                        if (unpaddedSamples.size < 3) unpaddedSamples.add("'$cell'")
                    }
                    if (cell.contains("  ")) {
                        multiSpaceCount++
                        multiSpaceCols.add(colName)
                    }
                }
            }
        }

        val textIssues = mutableListOf<QualityIssue>()
        if (unpaddedWhitespaceCount > 0) {
            textIssues.add(
                QualityIssue(
                    id = "whitespace_padding",
                    title = "Leading or Trailing Whitespace",
                    description = "$unpaddedWhitespaceCount cell(s) contain unpadded spaces before or after text.",
                    count = unpaddedWhitespaceCount,
                    severity = QualitySeverity.WARNING,
                    affectedColumns = whitespaceCols.toList(),
                    sampleValues = unpaddedSamples,
                    suggestedOperation = CleaningOperation.TrimWhitespace(whitespaceCols.toList(), trimAllEligible = true)
                )
            )
            whitespaceCols.forEach { recordAffectedCol(it) }
        }
        if (multiSpaceCount > 0) {
            textIssues.add(
                QualityIssue(
                    id = "internal_spaces",
                    title = "Repeated Internal Spaces",
                    description = "$multiSpaceCount cell(s) contain double or multiple spaces between words.",
                    count = multiSpaceCount,
                    severity = QualitySeverity.INFO,
                    affectedColumns = multiSpaceCols.toList(),
                    suggestedOperation = CleaningOperation.NormalizeInternalSpaces(multiSpaceCols.toList())
                )
            )
            multiSpaceCols.forEach { recordAffectedCol(it) }
        }

        // Inconsistent capitalization check
        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            val nonEmpties = dataset.rows.mapNotNull {
                val v = it.getOrNull(c)?.trim()
                if (v.isNullOrBlank() || v.length < 2) null else v
            }
            if (nonEmpties.size >= 4) {
                val lowerCount = nonEmpties.count { it == it.lowercase() }
                val upperCount = nonEmpties.count { it == it.uppercase() }
                val titleCount = nonEmpties.count { it[0].isUpperCase() && it.drop(1).all { ch -> !ch.isUpperCase() } }

                // If mixed significantly (e.g. at least 2 in one and 2 in another)
                val categories = listOf(lowerCount, upperCount, titleCount).count { it >= 2 }
                if (categories >= 2) {
                    textIssues.add(
                        QualityIssue(
                            id = "casing_$colName",
                            title = "Inconsistent Casing in '$colName'",
                            description = "Column has mixed lowercase ($lowerCount), uppercase ($upperCount), and mixed cases.",
                            count = nonEmpties.size,
                            severity = QualitySeverity.INFO,
                            affectedColumns = listOf(colName)
                        )
                    )
                    recordAffectedCol(colName)
                }
            }
        }

        // ==========================================
        // 4. NUMERIC & CURRENCY QUALITY
        // ==========================================
        val numericIssues = mutableListOf<QualityIssue>()
        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            val nonEmpties = dataset.rows.mapNotNull { it.getOrNull(c)?.trim() }.filter { it.isNotEmpty() }
            if (nonEmpties.size >= 3) {
                var cleanNumbers = 0
                var dirtyNumbers = 0 // Contains $, €, commas
                var nonNumericText = 0
                val dirtySamples = mutableListOf<String>()

                for (cell in nonEmpties) {
                    if (cell.toDoubleOrNull() != null) {
                        cleanNumbers++
                    } else {
                        val cleaned = cell.replace("$", "").replace("€", "").replace("£", "").replace(",", "").trim()
                        if (cleaned.toDoubleOrNull() != null) {
                            dirtyNumbers++
                            if (dirtySamples.size < 3) dirtySamples.add(cell)
                        } else {
                            nonNumericText++
                        }
                    }
                }

                // If majority is numeric-like but contains formatting or text
                if (dirtyNumbers > 0 && (cleanNumbers + dirtyNumbers) > nonNumericText) {
                    numericIssues.add(
                        QualityIssue(
                            id = "currency_symbols_$colName",
                            title = "Formatted Numbers or Currency in '$colName'",
                            description = "$dirtyNumbers cell(s) contain currency symbols or commas requiring numeric conversion.",
                            count = dirtyNumbers,
                            severity = QualitySeverity.WARNING,
                            affectedColumns = listOf(colName),
                            sampleValues = dirtySamples,
                            suggestedOperation = CleaningOperation.ConvertToNumeric(listOf(colName))
                        )
                    )
                    recordAffectedCol(colName, dirtyNumbers)
                } else if (cleanNumbers >= 3 && nonNumericText in 1..5) {
                    numericIssues.add(
                        QualityIssue(
                            id = "mixed_type_$colName",
                            title = "Invalid Text in Numeric Column '$colName'",
                            description = "$nonNumericText cell(s) in an otherwise numeric column contain non-numeric text.",
                            count = nonNumericText,
                            severity = QualitySeverity.ERROR,
                            affectedColumns = listOf(colName),
                            suggestedOperation = CleaningOperation.ConvertToNumeric(listOf(colName))
                        )
                    )
                    recordAffectedCol(colName, nonNumericText)
                }
            }
        }

        // ==========================================
        // 5. DATES QUALITY ANALYSIS
        // ==========================================
        val dateIssues = mutableListOf<QualityIssue>()
        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            val lower = colName.lowercase()
            if (lower.contains("date") || lower.contains("time") || lower.contains("created") || lower.contains("dob")) {
                val nonEmpties = dataset.rows.mapNotNull { it.getOrNull(c)?.trim() }.filter { it.isNotEmpty() }
                if (nonEmpties.isNotEmpty()) {
                    var recognizedIso = 0
                    var recognizedOther = 0
                    var unrecognized = 0
                    val samples = mutableListOf<String>()

                    for (cell in nonEmpties) {
                        when {
                            DATE_PATTERNS[0].matcher(cell).matches() -> recognizedIso++
                            DATE_PATTERNS.drop(1).any { it.matcher(cell).matches() } -> {
                                recognizedOther++
                                if (samples.size < 3) samples.add(cell)
                            }
                            else -> unrecognized++
                        }
                    }

                    if (recognizedOther > 0) {
                        dateIssues.add(
                            QualityIssue(
                                id = "mixed_dates_$colName",
                                title = "Mixed or Non-ISO Date Formats in '$colName'",
                                description = "$recognizedOther date(s) use non-standard formatting (e.g. MM/DD/YYYY).",
                                count = recognizedOther,
                                severity = QualitySeverity.WARNING,
                                affectedColumns = listOf(colName),
                                sampleValues = samples,
                                suggestedOperation = CleaningOperation.StandardizeDates(listOf(colName))
                            )
                        )
                        recordAffectedCol(colName, recognizedOther)
                    }
                }
            }
        }

        // ==========================================
        // 6. CONTACT DATA ANALYSIS
        // ==========================================
        val contactIssues = mutableListOf<QualityIssue>()
        for (c in 0 until totalCols) {
            val colName = dataset.columns[c]
            val lower = colName.lowercase()

            // Email check
            if (lower.contains("email")) {
                var invalidEmails = 0
                val invalidSamples = mutableListOf<String>()
                for (row in dataset.rows) {
                    val cell = row.getOrNull(c)?.trim().orEmpty()
                    if (cell.isNotEmpty() && !EMAIL_PATTERN.matcher(cell).matches()) {
                        invalidEmails++
                        if (invalidSamples.size < 3) invalidSamples.add(cell)
                    }
                }
                if (invalidEmails > 0) {
                    contactIssues.add(
                        QualityIssue(
                            id = "invalid_emails_$colName",
                            title = "Invalid Email Addresses in '$colName'",
                            description = "$invalidEmails cell(s) do not conform to standard email syntax.",
                            count = invalidEmails,
                            severity = QualitySeverity.ERROR,
                            affectedColumns = listOf(colName),
                            sampleValues = invalidSamples,
                            suggestedOperation = CleaningOperation.ValidateEmail(colName, EmailAction.TRIM_AND_LOWER)
                        )
                    )
                    recordAffectedCol(colName, invalidEmails)
                }
            }

            // Phone check
            if (lower.contains("phone") || lower.contains("tel") || lower.contains("mobile")) {
                var suspiciousPhones = 0
                val phoneSamples = mutableListOf<String>()
                for (row in dataset.rows) {
                    val cell = row.getOrNull(c)?.trim().orEmpty()
                    if (cell.isNotEmpty()) {
                        val digitCount = cell.count { it.isDigit() }
                        if (digitCount < 7 || digitCount > 15) {
                            suspiciousPhones++
                            if (phoneSamples.size < 3) phoneSamples.add(cell)
                        }
                    }
                }
                if (suspiciousPhones > 0) {
                    contactIssues.add(
                        QualityIssue(
                            id = "suspicious_phones_$colName",
                            title = "Suspicious Phone Numbers in '$colName'",
                            description = "$suspiciousPhones cell(s) have anomalous digit counts (<7 or >15 digits).",
                            count = suspiciousPhones,
                            severity = QualitySeverity.WARNING,
                            affectedColumns = listOf(colName),
                            sampleValues = phoneSamples,
                            suggestedOperation = CleaningOperation.FlagSuspiciousPhones(colName, PhoneAction.STANDARD_US)
                        )
                    )
                    recordAffectedCol(colName, suspiciousPhones)
                }
            }
        }

        // ==========================================
        // BUILD CATEGORIES & QUALITY SCORE
        // ==========================================
        val categories = mutableListOf<QualityIssueCategory>()
        if (missingIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Missing Data", "MissingData", missingIssues.sumOf { it.count }, missingIssues))
        }
        if (duplicatesIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Duplicates", "Duplicates", duplicatesIssues.sumOf { it.count }, duplicatesIssues))
        }
        if (textIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Text Quality", "TextQuality", textIssues.sumOf { it.count }, textIssues))
        }
        if (numericIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Numeric Quality", "Numeric", numericIssues.sumOf { it.count }, numericIssues))
        }
        if (dateIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Date Standardization", "Dates", dateIssues.sumOf { it.count }, dateIssues))
        }
        if (contactIssues.isNotEmpty()) {
            categories.add(QualityIssueCategory("Contact Data", "Contact", contactIssues.sumOf { it.count }, contactIssues))
        }

        val totalIssuesCount = categories.sumOf { it.totalCount }

        // Non-misleading documented Quality Score Formula:
        // Score = 100 - (emptyRatio * 30 + dupRatio * 30 + whitespaceRatio * 15 + errorRatio * 25)
        val emptyRatio = (emptyCellsCount.toDouble() / totalCells.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val dupRatio = (duplicateRowsCount.toDouble() / totalRows.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val whitespaceRatio = (unpaddedWhitespaceCount.toDouble() / totalCells.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val totalErrors = missingIssues.filter { it.severity == QualitySeverity.ERROR }.sumOf { it.count } +
                contactIssues.filter { it.severity == QualitySeverity.ERROR }.sumOf { it.count }
        val errorRatio = (totalErrors.toDouble() / totalRows.coerceAtLeast(1)).coerceIn(0.0, 1.0)

        val penalty = (emptyRatio * 30.0) + (dupRatio * 30.0) + (whitespaceRatio * 15.0) + (errorRatio * 25.0)
        val computedScore = (100.0 - penalty).toInt().coerceIn(10, 100)

        return DataQualityReport(
            qualityScore = computedScore,
            scoreFormula = "100 - (missingCells × 30% + duplicateRows × 30% + whitespace × 15% + criticalErrors × 25%)",
            totalIssuesCount = totalIssuesCount,
            categories = categories,
            affectedColumnsSummary = affectedColsMap,
            emptyRowsCount = emptyRowsCount,
            duplicateRowsCount = duplicateRowsCount,
            emptyCellsCount = emptyCellsCount,
            totalCellsCount = totalCells
        )
    }
}
