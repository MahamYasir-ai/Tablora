package com.example.model

/**
 * Core dataset representation. Rows are represented as lists of cell strings.
 * Leading zeros, special characters, and empty values are preserved accurately.
 */
data class SpreadsheetDataset(
    val name: String,
    val sheetName: String = "Sheet1",
    val availableSheets: List<String> = listOf("Sheet1"),
    val columns: List<String>,
    val rows: List<List<String>>,
    val originalRowCount: Int = rows.size,
    val originalColumnCount: Int = columns.size,
    val sourceFormat: String = "CSV"
) {
    val rowCount: Int get() = rows.size
    val columnCount: Int get() = columns.size

    fun getCellValue(rowIndex: Int, columnIndex: Int): String {
        if (rowIndex !in rows.indices) return ""
        val row = rows[rowIndex]
        return if (columnIndex in row.indices) row[columnIndex] else ""
    }
}

/**
 * Text case options for conversion.
 */
enum class TextCase(val label: String) {
    LOWER("lowercase"),
    UPPER("UPPERCASE"),
    TITLE("Title Case")
}

/**
 * Numeric invalid entry handling.
 */
enum class NumericInvalidHandling(val label: String) {
    SET_EMPTY("Set to empty/null"),
    KEEP_ORIGINAL("Keep original text"),
    SET_ZERO("Replace with 0")
}

/**
 * Target format for date standardization.
 */
enum class DateOutputFormat(val pattern: String, val label: String) {
    ISO_YMD("yyyy-MM-dd", "YYYY-MM-DD (ISO)"),
    US_MDY("MM/dd/yyyy", "MM/DD/YYYY (US)"),
    EU_DMY("dd/MM/yyyy", "DD/MM/YYYY (EU)"),
    TEXT_DATE("MMMM d, yyyy", "Month D, YYYY")
}

/**
 * Filter operators for row conditions.
 */
enum class FilterOperator(val label: String) {
    EQUALS("Equals"),
    CONTAINS("Contains"),
    STARTS_WITH("Starts with"),
    ENDS_WITH("Ends with"),
    IS_EMPTY("Is empty / blank"),
    IS_NOT_EMPTY("Is not empty"),
    GREATER_THAN("Numeric >"),
    LESS_THAN("Numeric <")
}

/**
 * Actions for email validation operation.
 */
enum class EmailAction(val label: String) {
    FLAG_INVALID("Flag invalid values"),
    TRIM_AND_LOWER("Trim spaces and convert to lowercase"),
    REMOVE_INVALID_ROWS("Remove rows with invalid emails")
}

/**
 * Actions for phone formatting.
 */
enum class PhoneAction(val label: String) {
    NORMALIZE_DIGITS("Extract digits only (e.g. 5551234567)"),
    STANDARD_US("Format as (XXX) XXX-XXXX"),
    FLAG_SUSPICIOUS("Flag entries with < 10 or > 15 digits")
}

/**
 * Missing value filling strategy.
 */
enum class FillStrategy(val label: String) {
    FIXED_VALUE("Custom fixed value"),
    FORWARD_FILL("Forward fill (previous non-empty)"),
    BACKWARD_FILL("Backward fill (next non-empty)"),
    NUMERIC_AVERAGE("Column numeric average"),
    PLACEHOLDER_NA("Set to 'N/A'")
}

/**
 * Sealed class representing all 22 distinct spreadsheet cleaning operations.
 */
sealed class CleaningOperation {
    abstract val displayName: String
    abstract val description: String

    // 1. Remove fully empty rows
    data object RemoveEmptyRows : CleaningOperation() {
        override val displayName = "Remove Empty Rows"
        override val description = "Discard rows where every cell is blank or whitespace."
    }

    // 2. Remove fully empty columns
    data object RemoveEmptyColumns : CleaningOperation() {
        override val displayName = "Remove Empty Columns"
        override val description = "Discard columns where every cell is blank or whitespace."
    }

    // 3. Remove exact duplicate rows
    data object RemoveExactDuplicates : CleaningOperation() {
        override val displayName = "Remove Exact Duplicate Rows"
        override val description = "Discard identical duplicate rows, keeping the first occurrence."
    }

    // 4. Remove duplicates based on user-selected columns
    data class RemoveDuplicatesByColumns(val targetColumns: List<String>) : CleaningOperation() {
        override val displayName = "Remove Duplicates by Column(s)"
        override val description = "Deduplicate based on: ${targetColumns.joinToString(", ")}"
    }

    // 5. Trim leading and trailing whitespace
    data class TrimWhitespace(val columns: List<String>, val trimAllEligible: Boolean = true) : CleaningOperation() {
        override val displayName = "Trim Whitespace"
        override val description = if (trimAllEligible) "Remove leading/trailing spaces from all text columns"
            else "Trim whitespace on: ${columns.joinToString(", ")}"
    }

    // 6. Normalize repeated internal spaces
    data class NormalizeInternalSpaces(val columns: List<String>) : CleaningOperation() {
        override val displayName = "Normalize Internal Spaces"
        override val description = "Collapse multiple consecutive spaces into a single space."
    }

    // 7, 8, 9. Convert selected text to lowercase, uppercase, or title case
    data class ConvertCase(val columns: List<String>, val targetCase: TextCase) : CleaningOperation() {
        override val displayName = "Convert Case (${targetCase.label})"
        override val description = "Transform text case to ${targetCase.label} on: ${columns.joinToString(", ")}"
    }

    // 10. Normalize blank strings to empty or null values
    data class NormalizeBlanks(val columns: List<String>, val treatWhitespaceOnlyAsEmpty: Boolean = true) : CleaningOperation() {
        override val displayName = "Normalize Blank Values"
        override val description = "Convert whitespace-only strings and 'null'/'None' strings into clean empty cells."
    }

    // 11. Convert selected columns to numeric types where conversion is valid
    data class ConvertToNumeric(
        val columns: List<String>,
        val stripCurrencyAndCommas: Boolean = true,
        val invalidHandling: NumericInvalidHandling = NumericInvalidHandling.KEEP_ORIGINAL
    ) : CleaningOperation() {
        override val displayName = "Convert to Numeric"
        override val description = "Clean numbers (strip \$, commas) on: ${columns.joinToString(", ")}"
    }

    // 12. Standardize recognized dates into a selected output format
    data class StandardizeDates(
        val columns: List<String>,
        val outputFormat: DateOutputFormat = DateOutputFormat.ISO_YMD
    ) : CleaningOperation() {
        override val displayName = "Standardize Dates"
        override val description = "Convert parsed dates into ${outputFormat.label} format."
    }

    // 13. Find and replace text
    data class FindAndReplace(
        val targetColumn: String?, // null means all columns
        val findText: String,
        val replaceText: String,
        val matchCase: Boolean = false,
        val entireCellMatch: Boolean = false
    ) : CleaningOperation() {
        override val displayName = "Find and Replace"
        override val description = "Replace '$findText' with '$replaceText' in ${targetColumn ?: "all columns"}"
    }

    // 14. Rename columns
    data class RenameColumn(val oldName: String, val newName: String) : CleaningOperation() {
        override val displayName = "Rename Column"
        override val description = "Rename '$oldName' to '$newName'"
    }

    // 15. Reorder columns
    data class ReorderColumns(val orderedColumns: List<String>) : CleaningOperation() {
        override val displayName = "Reorder Columns"
        override val description = "Reorder columns to custom sequence."
    }

    // 16. Select and remove columns
    data class RemoveColumns(val columnsToRemove: List<String>) : CleaningOperation() {
        override val displayName = "Remove Column(s)"
        override val description = "Delete columns: ${columnsToRemove.joinToString(", ")}"
    }

    // 17. Filter rows using explicit conditions
    data class FilterRows(
        val targetColumn: String,
        val operator: FilterOperator,
        val filterValue: String = "",
        val keepMatching: Boolean = true
    ) : CleaningOperation() {
        override val displayName = "Filter Rows (${operator.label})"
        override val description = "${if (keepMatching) "Keep" else "Discard"} rows where '$targetColumn' ${operator.label.lowercase()} '$filterValue'"
    }

    // 18. Validate email formats
    data class ValidateEmail(val columnName: String, val action: EmailAction) : CleaningOperation() {
        override val displayName = "Validate Email Formats"
        override val description = "${action.label} on column '$columnName'"
    }

    // 19. Flag suspicious phone numbers
    data class FlagSuspiciousPhones(val columnName: String, val action: PhoneAction) : CleaningOperation() {
        override val displayName = "Clean & Standardize Phones"
        override val description = "${action.label} on column '$columnName'"
    }

    // 20. Normalize selected text representations according to user mappings
    data class NormalizeMappings(
        val columnName: String,
        val mappings: Map<String, String>,
        val caseInsensitive: Boolean = true
    ) : CleaningOperation() {
        override val displayName = "Normalize Text Mappings"
        override val description = "Map ${mappings.size} variations to standard values in '$columnName'"
    }

    // 21. Trim selected columns or all eligible text columns
    data class TrimTextColumns(val columns: List<String>) : CleaningOperation() {
        override val displayName = "Trim Text Columns"
        override val description = "Trim leading/trailing spaces across ${columns.size} columns."
    }

    // 22. Fill missing values using a user-selected rule
    data class FillMissingValues(
        val targetColumns: List<String>,
        val strategy: FillStrategy,
        val customValue: String = ""
    ) : CleaningOperation() {
        override val displayName = "Fill Missing Values"
        override val description = "Fill blanks using ${strategy.label} on: ${targetColumns.joinToString(", ")}"
    }
}

/**
 * Result of previewing or applying an operation.
 */
data class OperationDiff(
    val operation: CleaningOperation,
    val rowsBefore: Int,
    val rowsAfter: Int,
    val columnsBefore: Int,
    val columnsAfter: Int,
    val modifiedCellsCount: Int,
    val previewRows: List<List<String>> = emptyList(),
    val summaryMessage: String
)

/**
 * Severity level for detected data quality issues.
 */
enum class QualitySeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * Data quality issue details.
 */
data class QualityIssue(
    val id: String,
    val title: String,
    val description: String,
    val count: Int,
    val severity: QualitySeverity,
    val affectedColumns: List<String>,
    val sampleValues: List<String> = emptyList(),
    val suggestedOperation: CleaningOperation? = null
)

/**
 * Grouped quality issue category.
 */
data class QualityIssueCategory(
    val name: String,
    val iconName: String,
    val totalCount: Int,
    val issues: List<QualityIssue>
)

/**
 * Full data quality report.
 */
data class DataQualityReport(
    val qualityScore: Int, // 0 - 100
    val scoreFormula: String,
    val totalIssuesCount: Int,
    val categories: List<QualityIssueCategory>,
    val affectedColumnsSummary: Map<String, Int>,
    val emptyRowsCount: Int,
    val duplicateRowsCount: Int,
    val emptyCellsCount: Int,
    val totalCellsCount: Int
)

/**
 * Reusable cleaning preset.
 */
data class CleaningPreset(
    val id: String,
    val name: String,
    val description: String,
    val operations: List<CleaningOperation>,
    val isBuiltIn: Boolean = false,
    val requiredColumns: List<String> = emptyList()
)

/**
 * Completed job session record.
 */
data class JobHistoryItem(
    val id: String,
    val timestamp: Long,
    val filename: String,
    val originalRows: Int,
    val originalCols: Int,
    val finalRows: Int,
    val finalCols: Int,
    val operationsCount: Int,
    val exportFormat: String,
    val completedExport: Boolean
)

/**
 * AI Assistant proposed plan.
 */
data class AiProposedPlan(
    val userPrompt: String,
    val planSummary: String,
    val rationale: String,
    val operations: List<CleaningOperation>,
    val requiresApproval: Boolean = true
)

/**
 * Table density preferences.
 */
enum class TableDensity(val label: String, val rowHeightDp: Int) {
    COMPACT("Compact", 36),
    NORMAL("Normal", 46),
    COMFORTABLE("Comfortable", 56)
}

/**
 * Export format options.
 */
enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    CSV("csv", "text/csv", "CSV (Comma-Separated)"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Workbook (.xlsx)")
}

/**
 * CSV delimiter options.
 */
enum class CsvDelimiter(val char: Char, val label: String) {
    COMMA(',', "Comma (,)"),
    SEMICOLON(';', "Semicolon (;)"),
    TAB('\t', "Tab (\\t)"),
    PIPE('|', "Pipe (|)")
}

/**
 * Application screen routes.
 */
enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    NEW_JOB("New Job"),
    QUALITY_REPORT("Data Quality"),
    WORKSPACE("Cleaning Workspace"),
    PRESETS("Saved Presets"),
    HISTORY("Job History"),
    PRICING("Pricing"),
    SETTINGS("Settings")
}
