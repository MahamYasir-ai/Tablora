package com.example.engine

import com.example.model.CleaningOperation
import com.example.model.DateOutputFormat
import com.example.model.EmailAction
import com.example.model.FillStrategy
import com.example.model.FilterOperator
import com.example.model.NumericInvalidHandling
import com.example.model.OperationDiff
import com.example.model.PhoneAction
import com.example.model.SpreadsheetDataset
import com.example.model.TextCase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * Pure, deterministic transformation engine executing spreadsheet cleaning operations.
 * Never silently destroys data; provides detailed preview diffs.
 */
object CleaningEngine {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )

    /**
     * Preview an operation without mutating the actual dataset.
     */
    fun preview(dataset: SpreadsheetDataset, operation: CleaningOperation): OperationDiff {
        val (resultDataset, modifiedCells) = executeInternal(dataset, operation)

        val rowDelta = dataset.rowCount - resultDataset.rowCount
        val colDelta = dataset.columnCount - resultDataset.columnCount

        val summary = when {
            rowDelta > 0 && colDelta > 0 -> "Removed $rowDelta row(s) and $colDelta column(s)."
            rowDelta > 0 -> "Removed $rowDelta row(s)."
            colDelta > 0 -> "Removed $colDelta column(s)."
            modifiedCells > 0 -> "Modified $modifiedCells cell(s)."
            else -> "No rows or cells were modified."
        }

        return OperationDiff(
            operation = operation,
            rowsBefore = dataset.rowCount,
            rowsAfter = resultDataset.rowCount,
            columnsBefore = dataset.columnCount,
            columnsAfter = resultDataset.columnCount,
            modifiedCellsCount = modifiedCells,
            previewRows = resultDataset.rows.take(15),
            summaryMessage = summary
        )
    }

    /**
     * Apply operation and return new transformed dataset.
     */
    fun apply(dataset: SpreadsheetDataset, operation: CleaningOperation): SpreadsheetDataset {
        return executeInternal(dataset, operation).first
    }

    private fun executeInternal(
        dataset: SpreadsheetDataset,
        operation: CleaningOperation
    ): Pair<SpreadsheetDataset, Int> {
        val currentCols = dataset.columns
        val currentRows = dataset.rows

        return when (operation) {
            is CleaningOperation.RemoveEmptyRows -> {
                val filtered = currentRows.filter { row -> row.any { it.isNotBlank() } }
                val modified = (currentRows.size - filtered.size) * currentCols.size
                dataset.copy(rows = filtered) to modified
            }

            is CleaningOperation.RemoveEmptyColumns -> {
                val keepIndices = currentCols.indices.filter { c ->
                    currentRows.any { row -> row.getOrNull(c)?.isNotBlank() == true }
                }
                val newCols = keepIndices.map { currentCols[it] }
                val newRows = currentRows.map { row -> keepIndices.map { idx -> row.getOrElse(idx) { "" } } }
                val droppedCols = currentCols.size - newCols.size
                dataset.copy(columns = newCols, rows = newRows) to (droppedCols * currentRows.size)
            }

            is CleaningOperation.RemoveExactDuplicates -> {
                val seen = mutableSetOf<String>()
                val newRows = mutableListOf<List<String>>()
                for (row in currentRows) {
                    val key = row.joinToString("\u0000")
                    if (seen.add(key)) {
                        newRows.add(row)
                    }
                }
                val droppedRows = currentRows.size - newRows.size
                dataset.copy(rows = newRows) to (droppedRows * currentCols.size)
            }

            is CleaningOperation.RemoveDuplicatesByColumns -> {
                val colIndices = operation.targetColumns.map { currentCols.indexOf(it) }.filter { it != -1 }
                if (colIndices.isEmpty()) return dataset to 0

                val seen = mutableSetOf<String>()
                val newRows = mutableListOf<List<String>>()
                for (row in currentRows) {
                    val key = colIndices.joinToString("\u0000") { idx -> row.getOrElse(idx) { "" }.trim() }
                    if (key.isBlank() || seen.add(key)) {
                        newRows.add(row)
                    }
                }
                val droppedRows = currentRows.size - newRows.size
                dataset.copy(rows = newRows) to (droppedRows * currentCols.size)
            }

            is CleaningOperation.TrimWhitespace -> {
                val targetIndices = if (operation.trimAllEligible) {
                    currentCols.indices.toList()
                } else {
                    operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                }

                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices) {
                            val trimmed = cell.trim()
                            if (trimmed != cell) modCount++
                            trimmed
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.TrimTextColumns -> {
                val targetIndices = operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices) {
                            val trimmed = cell.trim()
                            if (trimmed != cell) modCount++
                            trimmed
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.NormalizeInternalSpaces -> {
                val targetIndices = operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices && cell.contains("  ")) {
                            val normalized = cell.replace(Regex("""\s+"""), " ")
                            if (normalized != cell) modCount++
                            normalized
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.ConvertCase -> {
                val targetIndices = operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices && cell.isNotEmpty()) {
                            val transformed = when (operation.targetCase) {
                                TextCase.LOWER -> cell.lowercase()
                                TextCase.UPPER -> cell.uppercase()
                                TextCase.TITLE -> toTitleCase(cell)
                            }
                            if (transformed != cell) modCount++
                            transformed
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.NormalizeBlanks -> {
                val targetIndices = if (operation.columns.isEmpty()) currentCols.indices.toList()
                else operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }

                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices) {
                            val trimmed = cell.trim()
                            val isBlankish = trimmed.isEmpty() ||
                                    trimmed.equals("null", ignoreCase = true) ||
                                    trimmed.equals("none", ignoreCase = true) ||
                                    trimmed.equals("n/a", ignoreCase = true)
                            if (isBlankish && cell != "") {
                                modCount++
                                ""
                            } else cell
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.ConvertToNumeric -> {
                val targetIndices = operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices && cell.isNotEmpty()) {
                            var cleanStr = cell.trim()
                            if (operation.stripCurrencyAndCommas) {
                                cleanStr = cleanStr.replace("$", "")
                                    .replace("€", "")
                                    .replace("£", "")
                                    .replace(",", "")
                                    .trim()
                            }
                            val num = cleanStr.toDoubleOrNull()
                            val finalVal = if (num != null) {
                                // Format cleanly: if integer value, strip .0
                                if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                            } else {
                                when (operation.invalidHandling) {
                                    NumericInvalidHandling.SET_EMPTY -> ""
                                    NumericInvalidHandling.KEEP_ORIGINAL -> cell
                                    NumericInvalidHandling.SET_ZERO -> "0"
                                }
                            }
                            if (finalVal != cell) modCount++
                            finalVal
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.StandardizeDates -> {
                val targetIndices = operation.columns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0
                val targetSdf = SimpleDateFormat(operation.outputFormat.pattern, Locale.US)

                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices && cell.isNotBlank()) {
                            val parsedDate = tryParseDate(cell.trim())
                            if (parsedDate != null) {
                                val formatted = targetSdf.format(parsedDate)
                                if (formatted != cell) modCount++
                                formatted
                            } else cell
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.FindAndReplace -> {
                val targetIndices = if (operation.targetColumn == null) currentCols.indices.toList()
                else listOf(currentCols.indexOf(operation.targetColumn)).filter { it != -1 }

                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c in targetIndices) {
                            val replaced = if (operation.entireCellMatch) {
                                val matches = if (operation.matchCase) cell == operation.findText
                                else cell.equals(operation.findText, ignoreCase = true)
                                if (matches) operation.replaceText else cell
                            } else {
                                if (operation.matchCase) {
                                    cell.replace(operation.findText, operation.replaceText)
                                } else {
                                    cell.replace(Regex(Pattern.quote(operation.findText), RegexOption.IGNORE_CASE), operation.replaceText)
                                }
                            }
                            if (replaced != cell) modCount++
                            replaced
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.RenameColumn -> {
                val idx = currentCols.indexOf(operation.oldName)
                if (idx == -1) return dataset to 0
                val newCols = currentCols.toMutableList()
                newCols[idx] = operation.newName
                dataset.copy(columns = newCols) to 1
            }

            is CleaningOperation.ReorderColumns -> {
                val newCols = operation.orderedColumns.filter { it in currentCols }
                val remaining = currentCols.filter { it !in newCols }
                val finalCols = newCols + remaining
                val indices = finalCols.map { currentCols.indexOf(it) }

                val newRows = currentRows.map { row ->
                    indices.map { idx -> row.getOrElse(idx) { "" } }
                }
                dataset.copy(columns = finalCols, rows = newRows) to currentRows.size
            }

            is CleaningOperation.RemoveColumns -> {
                val removeSet = operation.columnsToRemove.toSet()
                val keepIndices = currentCols.indices.filter { currentCols[it] !in removeSet }
                val newCols = keepIndices.map { currentCols[it] }
                val newRows = currentRows.map { row -> keepIndices.map { idx -> row.getOrElse(idx) { "" } } }
                dataset.copy(columns = newCols, rows = newRows) to (removeSet.size * currentRows.size)
            }

            is CleaningOperation.FilterRows -> {
                val colIdx = currentCols.indexOf(operation.targetColumn)
                if (colIdx == -1) return dataset to 0

                val filtered = currentRows.filter { row ->
                    val cellVal = row.getOrElse(colIdx) { "" }
                    val matches = evaluateFilter(cellVal, operation.operator, operation.filterValue)
                    if (operation.keepMatching) matches else !matches
                }
                val dropped = currentRows.size - filtered.size
                dataset.copy(rows = filtered) to (dropped * currentCols.size)
            }

            is CleaningOperation.ValidateEmail -> {
                val colIdx = currentCols.indexOf(operation.columnName)
                if (colIdx == -1) return dataset to 0

                var modCount = 0
                when (operation.action) {
                    EmailAction.FLAG_INVALID -> {
                        val newRows = currentRows.map { row ->
                            row.mapIndexed { c, cell ->
                                if (c == colIdx && cell.isNotBlank()) {
                                    val trimmed = cell.trim()
                                    if (!EMAIL_PATTERN.matcher(trimmed).matches() && !cell.startsWith("[INVALID]")) {
                                        modCount++
                                        "[INVALID] $cell"
                                    } else cell
                                } else cell
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }
                    EmailAction.TRIM_AND_LOWER -> {
                        val newRows = currentRows.map { row ->
                            row.mapIndexed { c, cell ->
                                if (c == colIdx && cell.isNotBlank()) {
                                    val cleaned = cell.trim().lowercase()
                                    if (cleaned != cell) modCount++
                                    cleaned
                                } else cell
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }
                    EmailAction.REMOVE_INVALID_ROWS -> {
                        val filtered = currentRows.filter { row ->
                            val cell = row.getOrElse(colIdx) { "" }.trim()
                            cell.isBlank() || EMAIL_PATTERN.matcher(cell).matches()
                        }
                        val dropped = currentRows.size - filtered.size
                        dataset.copy(rows = filtered) to (dropped * currentCols.size)
                    }
                }
            }

            is CleaningOperation.FlagSuspiciousPhones -> {
                val colIdx = currentCols.indexOf(operation.columnName)
                if (colIdx == -1) return dataset to 0

                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c == colIdx && cell.isNotBlank()) {
                            val digits = cell.filter { it.isDigit() }
                            val transformed = when (operation.action) {
                                PhoneAction.NORMALIZE_DIGITS -> digits
                                PhoneAction.STANDARD_US -> {
                                    if (digits.length == 10) {
                                        "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
                                    } else if (digits.length == 11 && digits.startsWith("1")) {
                                        "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
                                    } else cell
                                }
                                PhoneAction.FLAG_SUSPICIOUS -> {
                                    if (digits.length !in 10..15 && !cell.startsWith("[SUSPICIOUS]")) {
                                        "[SUSPICIOUS] $cell"
                                    } else cell
                                }
                            }
                            if (transformed != cell) modCount++
                            transformed
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.NormalizeMappings -> {
                val colIdx = currentCols.indexOf(operation.columnName)
                if (colIdx == -1) return dataset to 0

                var modCount = 0
                val newRows = currentRows.map { row ->
                    row.mapIndexed { c, cell ->
                        if (c == colIdx && cell.isNotEmpty()) {
                            val key = if (operation.caseInsensitive) cell.trim().lowercase() else cell.trim()
                            val target = if (operation.caseInsensitive) {
                                operation.mappings.entries.find { it.key.trim().equals(key, ignoreCase = true) }?.value
                            } else {
                                operation.mappings[key]
                            }
                            if (target != null && target != cell) {
                                modCount++
                                target
                            } else cell
                        } else cell
                    }
                }
                dataset.copy(rows = newRows) to modCount
            }

            is CleaningOperation.FillMissingValues -> {
                val targetIndices = operation.targetColumns.map { currentCols.indexOf(it) }.filter { it != -1 }
                var modCount = 0

                when (operation.strategy) {
                    FillStrategy.FIXED_VALUE, FillStrategy.PLACEHOLDER_NA -> {
                        val fillVal = if (operation.strategy == FillStrategy.PLACEHOLDER_NA) "N/A" else operation.customValue
                        val newRows = currentRows.map { row ->
                            row.mapIndexed { c, cell ->
                                if (c in targetIndices && cell.isBlank()) {
                                    modCount++
                                    fillVal
                                } else cell
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }

                    FillStrategy.FORWARD_FILL -> {
                        val lastVal = mutableMapOf<Int, String>()
                        val newRows = currentRows.map { row ->
                            row.mapIndexed { c, cell ->
                                if (c in targetIndices) {
                                    if (cell.isNotBlank()) {
                                        lastVal[c] = cell
                                        cell
                                    } else {
                                        val prev = lastVal[c]
                                        if (prev != null) {
                                            modCount++
                                            prev
                                        } else cell
                                    }
                                } else cell
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }

                    FillStrategy.BACKWARD_FILL -> {
                        val newRows = currentRows.toMutableList()
                        for (c in targetIndices) {
                            var nextVal: String? = null
                            for (r in currentRows.indices.reversed()) {
                                val cell = newRows[r][c]
                                if (cell.isNotBlank()) {
                                    nextVal = cell
                                } else if (nextVal != null) {
                                    val updatedRow = newRows[r].toMutableList()
                                    updatedRow[c] = nextVal
                                    newRows[r] = updatedRow
                                    modCount++
                                }
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }

                    FillStrategy.NUMERIC_AVERAGE -> {
                        // Compute mean for each target column
                        val averages = mutableMapOf<Int, String>()
                        for (c in targetIndices) {
                            val numbers = currentRows.mapNotNull { it.getOrNull(c)?.toDoubleOrNull() }
                            if (numbers.isNotEmpty()) {
                                val avg = numbers.average()
                                averages[c] = if (avg % 1.0 == 0.0) avg.toLong().toString() else String.format(Locale.US, "%.2f", avg)
                            }
                        }

                        val newRows = currentRows.map { row ->
                            row.mapIndexed { c, cell ->
                                if (c in targetIndices && cell.isBlank() && averages.containsKey(c)) {
                                    modCount++
                                    averages[c]!!
                                } else cell
                            }
                        }
                        dataset.copy(rows = newRows) to modCount
                    }
                }
            }
        }
    }

    private fun evaluateFilter(cell: String, op: FilterOperator, filterVal: String): Boolean {
        return when (op) {
            FilterOperator.EQUALS -> cell.equals(filterVal, ignoreCase = true)
            FilterOperator.CONTAINS -> cell.contains(filterVal, ignoreCase = true)
            FilterOperator.STARTS_WITH -> cell.startsWith(filterVal, ignoreCase = true)
            FilterOperator.ENDS_WITH -> cell.endsWith(filterVal, ignoreCase = true)
            FilterOperator.IS_EMPTY -> cell.isBlank()
            FilterOperator.IS_NOT_EMPTY -> cell.isNotBlank()
            FilterOperator.GREATER_THAN -> {
                val num = cell.toDoubleOrNull()
                val targetNum = filterVal.toDoubleOrNull()
                if (num != null && targetNum != null) num > targetNum else false
            }
            FilterOperator.LESS_THAN -> {
                val num = cell.toDoubleOrNull()
                val targetNum = filterVal.toDoubleOrNull()
                if (num != null && targetNum != null) num < targetNum else false
            }
        }
    }

    private fun toTitleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            if (word.length <= 1) word.uppercase()
            else word[0].uppercaseChar() + word.substring(1).lowercase()
        }
    }

    private fun tryParseDate(str: String): java.util.Date? {
        val formats = listOf(
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy/MM/dd",
            "MM-dd-yyyy",
            "dd-MM-yyyy",
            "MMMM d, yyyy",
            "MMM d, yyyy",
            "d MMMM yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                return sdf.parse(str)
            } catch (_: Exception) {
                // Next format
            }
        }
        return null
    }
}
