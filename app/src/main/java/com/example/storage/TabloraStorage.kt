package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CleaningOperation
import com.example.model.CleaningPreset
import com.example.model.CsvDelimiter
import com.example.model.DateOutputFormat
import com.example.model.EmailAction
import com.example.model.ExportFormat
import com.example.model.FillStrategy
import com.example.model.FilterOperator
import com.example.model.JobHistoryItem
import com.example.model.NumericInvalidHandling
import com.example.model.PhoneAction
import com.example.model.TableDensity
import com.example.model.TextCase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Handles browser-equivalent local persistence using Android SharedPreferences.
 * Stores non-sensitive metadata only: saved presets, past job summaries, and UI preferences.
 * Does NOT persist uploaded spreadsheet cell contents.
 */
class TabloraStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tablora_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRESETS = "key_presets"
        private const val KEY_HISTORY = "key_history"
        private const val KEY_THEME = "key_theme"
        private const val KEY_DENSITY = "key_density"
        private const val KEY_DEFAULT_EXPORT = "key_default_export"
        private const val KEY_DEFAULT_DELIMITER = "key_default_delimiter"
    }

    // -------------------------------------------------------------
    // PRESETS
    // -------------------------------------------------------------

    fun getPresets(): List<CleaningPreset> {
        val rawJson = prefs.getString(KEY_PRESETS, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = getBuiltInPresets()
            savePresets(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<CleaningPreset>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(deserializePreset(obj))
            }
            if (list.isEmpty()) getBuiltInPresets() else list
        } catch (_: Exception) {
            getBuiltInPresets()
        }
    }

    fun savePreset(preset: CleaningPreset) {
        val current = getPresets().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            current[index] = preset
        } else {
            current.add(0, preset)
        }
        savePresets(current)
    }

    fun deletePreset(presetId: String) {
        val current = getPresets().filterNot { it.id == presetId && !it.isBuiltIn }
        savePresets(current)
    }

    fun resetPresetsToDefault() {
        savePresets(getBuiltInPresets())
    }

    private fun savePresets(presets: List<CleaningPreset>) {
        val arr = JSONArray()
        for (p in presets) {
            arr.put(serializePreset(p))
        }
        prefs.edit().putString(KEY_PRESETS, arr.toString()).apply()
    }

    private fun getBuiltInPresets(): List<CleaningPreset> {
        return listOf(
            CleaningPreset(
                id = "preset_clean_customer_list",
                name = "Clean Customer List",
                description = "Removes empty & duplicate rows, trims whitespace, and standardizes emails.",
                isBuiltIn = true,
                operations = listOf(
                    CleaningOperation.RemoveEmptyRows,
                    CleaningOperation.RemoveExactDuplicates,
                    CleaningOperation.TrimWhitespace(emptyList(), trimAllEligible = true),
                    CleaningOperation.NormalizeInternalSpaces(emptyList())
                )
            ),
            CleaningPreset(
                id = "preset_prepare_sales_data",
                name = "Prepare Sales Data",
                description = "Removes blanks, strips currency symbols, and cleans numeric fields.",
                isBuiltIn = true,
                operations = listOf(
                    CleaningOperation.RemoveEmptyRows,
                    CleaningOperation.RemoveExactDuplicates,
                    CleaningOperation.TrimWhitespace(emptyList(), trimAllEligible = true),
                    CleaningOperation.NormalizeBlanks(emptyList())
                )
            ),
            CleaningPreset(
                id = "preset_standardize_contacts",
                name = "Standardize Contact Information",
                description = "Trims names, formats phone numbers, and lowercases emails.",
                isBuiltIn = true,
                operations = listOf(
                    CleaningOperation.TrimWhitespace(emptyList(), trimAllEligible = true),
                    CleaningOperation.NormalizeInternalSpaces(emptyList())
                )
            ),
            CleaningPreset(
                id = "preset_remove_empty_records",
                name = "Remove Empty Records",
                description = "Discards fully empty rows and columns, converting blanks to clean nulls.",
                isBuiltIn = true,
                operations = listOf(
                    CleaningOperation.RemoveEmptyRows,
                    CleaningOperation.RemoveEmptyColumns,
                    CleaningOperation.NormalizeBlanks(emptyList())
                )
            ),
            CleaningPreset(
                id = "preset_normalize_text_columns",
                name = "Normalize Text Columns",
                description = "Trims spaces, collapses internal doubles, and converts names to Title Case.",
                isBuiltIn = true,
                operations = listOf(
                    CleaningOperation.TrimWhitespace(emptyList(), trimAllEligible = true),
                    CleaningOperation.NormalizeInternalSpaces(emptyList())
                )
            )
        )
    }

    // -------------------------------------------------------------
    // JOB HISTORY
    // -------------------------------------------------------------

    fun getHistory(): List<JobHistoryItem> {
        val rawJson = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<JobHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    JobHistoryItem(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        filename = obj.getString("filename"),
                        originalRows = obj.getInt("originalRows"),
                        originalCols = obj.getInt("originalCols"),
                        finalRows = obj.getInt("finalRows"),
                        finalCols = obj.getInt("finalCols"),
                        operationsCount = obj.getInt("operationsCount"),
                        exportFormat = obj.getString("exportFormat"),
                        completedExport = obj.getBoolean("completedExport")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistoryItem(item: JobHistoryItem) {
        val current = getHistory().toMutableList()
        current.add(0, item)
        val trimmed = current.take(50) // Keep last 50 jobs
        val arr = JSONArray()
        for (h in trimmed) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("timestamp", h.timestamp)
            obj.put("filename", h.filename)
            obj.put("originalRows", h.originalRows)
            obj.put("originalCols", h.originalCols)
            obj.put("finalRows", h.finalRows)
            obj.put("finalCols", h.finalCols)
            obj.put("operationsCount", h.operationsCount)
            obj.put("exportFormat", h.exportFormat)
            obj.put("completedExport", h.completedExport)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun deleteHistoryItem(id: String) {
        val current = getHistory().filterNot { it.id == id }
        val arr = JSONArray()
        for (h in current) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("timestamp", h.timestamp)
            obj.put("filename", h.filename)
            obj.put("originalRows", h.originalRows)
            obj.put("originalCols", h.originalCols)
            obj.put("finalRows", h.finalRows)
            obj.put("finalCols", h.finalCols)
            obj.put("operationsCount", h.operationsCount)
            obj.put("exportFormat", h.exportFormat)
            obj.put("completedExport", h.completedExport)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        resetPresetsToDefault()
    }

    // -------------------------------------------------------------
    // PREFERENCES
    // -------------------------------------------------------------

    fun getTableDensity(): TableDensity {
        val name = prefs.getString(KEY_DENSITY, TableDensity.NORMAL.name)
        return try {
            TableDensity.valueOf(name ?: TableDensity.NORMAL.name)
        } catch (_: Exception) {
            TableDensity.NORMAL
        }
    }

    fun setTableDensity(density: TableDensity) {
        prefs.edit().putString(KEY_DENSITY, density.name).apply()
    }

    fun getDefaultExportFormat(): ExportFormat {
        val name = prefs.getString(KEY_DEFAULT_EXPORT, ExportFormat.CSV.name)
        return try {
            ExportFormat.valueOf(name ?: ExportFormat.CSV.name)
        } catch (_: Exception) {
            ExportFormat.CSV
        }
    }

    fun setDefaultExportFormat(format: ExportFormat) {
        prefs.edit().putString(KEY_DEFAULT_EXPORT, format.name).apply()
    }

    fun getDefaultCsvDelimiter(): CsvDelimiter {
        val name = prefs.getString(KEY_DEFAULT_DELIMITER, CsvDelimiter.COMMA.name)
        return try {
            CsvDelimiter.valueOf(name ?: CsvDelimiter.COMMA.name)
        } catch (_: Exception) {
            CsvDelimiter.COMMA
        }
    }

    fun setDefaultCsvDelimiter(delimiter: CsvDelimiter) {
        prefs.edit().putString(KEY_DEFAULT_DELIMITER, delimiter.name).apply()
    }

    // -------------------------------------------------------------
    // SERIALIZATION HELPERS
    // -------------------------------------------------------------

    private fun serializePreset(p: CleaningPreset): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("description", p.description)
        obj.put("isBuiltIn", p.isBuiltIn)

        val opsArr = JSONArray()
        for (op in p.operations) {
            val opObj = JSONObject()
            opObj.put("type", op::class.java.simpleName)
            when (op) {
                is CleaningOperation.RemoveDuplicatesByColumns -> {
                    opObj.put("targetColumns", JSONArray(op.targetColumns))
                }
                is CleaningOperation.TrimWhitespace -> {
                    opObj.put("columns", JSONArray(op.columns))
                    opObj.put("trimAllEligible", op.trimAllEligible)
                }
                is CleaningOperation.ConvertCase -> {
                    opObj.put("columns", JSONArray(op.columns))
                    opObj.put("targetCase", op.targetCase.name)
                }
                is CleaningOperation.StandardizeDates -> {
                    opObj.put("columns", JSONArray(op.columns))
                    opObj.put("outputFormat", op.outputFormat.name)
                }
                is CleaningOperation.FindAndReplace -> {
                    opObj.put("targetColumn", op.targetColumn ?: "")
                    opObj.put("findText", op.findText)
                    opObj.put("replaceText", op.replaceText)
                    opObj.put("matchCase", op.matchCase)
                    opObj.put("entireCellMatch", op.entireCellMatch)
                }
                is CleaningOperation.RenameColumn -> {
                    opObj.put("oldName", op.oldName)
                    opObj.put("newName", op.newName)
                }
                is CleaningOperation.RemoveColumns -> {
                    opObj.put("columnsToRemove", JSONArray(op.columnsToRemove))
                }
                is CleaningOperation.FilterRows -> {
                    opObj.put("targetColumn", op.targetColumn)
                    opObj.put("operator", op.operator.name)
                    opObj.put("filterValue", op.filterValue)
                    opObj.put("keepMatching", op.keepMatching)
                }
                is CleaningOperation.ValidateEmail -> {
                    opObj.put("columnName", op.columnName)
                    opObj.put("action", op.action.name)
                }
                is CleaningOperation.FlagSuspiciousPhones -> {
                    opObj.put("columnName", op.columnName)
                    opObj.put("action", op.action.name)
                }
                else -> { /* No extra fields */ }
            }
            opsArr.put(opObj)
        }
        obj.put("operations", opsArr)
        return obj
    }

    private fun deserializePreset(obj: JSONObject): CleaningPreset {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val description = obj.getString("description")
        val isBuiltIn = obj.optBoolean("isBuiltIn", false)

        val ops = mutableListOf<CleaningOperation>()
        val opsArr = obj.optJSONArray("operations") ?: JSONArray()

        for (i in 0 until opsArr.length()) {
            val opObj = opsArr.getJSONObject(i)
            val type = opObj.getString("type")
            when (type) {
                "RemoveEmptyRows" -> ops.add(CleaningOperation.RemoveEmptyRows)
                "RemoveEmptyColumns" -> ops.add(CleaningOperation.RemoveEmptyColumns)
                "RemoveExactDuplicates" -> ops.add(CleaningOperation.RemoveExactDuplicates)
                "RemoveDuplicatesByColumns" -> {
                    val cols = jsonArrayToList(opObj.optJSONArray("targetColumns"))
                    ops.add(CleaningOperation.RemoveDuplicatesByColumns(cols))
                }
                "TrimWhitespace" -> {
                    val cols = jsonArrayToList(opObj.optJSONArray("columns"))
                    val all = opObj.optBoolean("trimAllEligible", true)
                    ops.add(CleaningOperation.TrimWhitespace(cols, all))
                }
                "NormalizeInternalSpaces" -> {
                    val cols = jsonArrayToList(opObj.optJSONArray("columns"))
                    ops.add(CleaningOperation.NormalizeInternalSpaces(cols))
                }
                "ConvertCase" -> {
                    val cols = jsonArrayToList(opObj.optJSONArray("columns"))
                    val tc = try { TextCase.valueOf(opObj.optString("targetCase")) } catch (_: Exception) { TextCase.LOWER }
                    ops.add(CleaningOperation.ConvertCase(cols, tc))
                }
                "NormalizeBlanks" -> ops.add(CleaningOperation.NormalizeBlanks(emptyList()))
                "ConvertToNumeric" -> ops.add(CleaningOperation.ConvertToNumeric(emptyList()))
                "StandardizeDates" -> {
                    val cols = jsonArrayToList(opObj.optJSONArray("columns"))
                    val fmt = try { DateOutputFormat.valueOf(opObj.optString("outputFormat")) } catch (_: Exception) { DateOutputFormat.ISO_YMD }
                    ops.add(CleaningOperation.StandardizeDates(cols, fmt))
                }
                "FindAndReplace" -> {
                    ops.add(
                        CleaningOperation.FindAndReplace(
                            targetColumn = opObj.optString("targetColumn").ifEmpty { null },
                            findText = opObj.optString("findText"),
                            replaceText = opObj.optString("replaceText"),
                            matchCase = opObj.optBoolean("matchCase", false),
                            entireCellMatch = opObj.optBoolean("entireCellMatch", false)
                        )
                    )
                }
                "RenameColumn" -> ops.add(CleaningOperation.RenameColumn(opObj.optString("oldName"), opObj.optString("newName")))
                "RemoveColumns" -> ops.add(CleaningOperation.RemoveColumns(jsonArrayToList(opObj.optJSONArray("columnsToRemove"))))
                "FilterRows" -> {
                    val op = try { FilterOperator.valueOf(opObj.optString("operator")) } catch (_: Exception) { FilterOperator.EQUALS }
                    ops.add(CleaningOperation.FilterRows(opObj.optString("targetColumn"), op, opObj.optString("filterValue"), opObj.optBoolean("keepMatching", true)))
                }
                "ValidateEmail" -> {
                    val act = try { EmailAction.valueOf(opObj.optString("action")) } catch (_: Exception) { EmailAction.TRIM_AND_LOWER }
                    ops.add(CleaningOperation.ValidateEmail(opObj.optString("columnName"), act))
                }
                "FlagSuspiciousPhones" -> {
                    val act = try { PhoneAction.valueOf(opObj.optString("action")) } catch (_: Exception) { PhoneAction.STANDARD_US }
                    ops.add(CleaningOperation.FlagSuspiciousPhones(opObj.optString("columnName"), act))
                }
                else -> { /* Unknown op skipped */ }
            }
        }

        return CleaningPreset(
            id = id,
            name = name,
            description = description,
            operations = ops,
            isBuiltIn = isBuiltIn
        )
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }
}
