package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AiCleaningAssistant
import com.example.engine.CleaningEngine
import com.example.engine.DataQualityAnalyzer
import com.example.engine.SampleDataRepository
import com.example.engine.SpreadsheetEngine
import com.example.model.AiProposedPlan
import com.example.model.AppScreen
import com.example.model.CleaningOperation
import com.example.model.CleaningPreset
import com.example.model.CsvDelimiter
import com.example.model.DataQualityReport
import com.example.model.ExportFormat
import com.example.model.JobHistoryItem
import com.example.model.OperationDiff
import com.example.model.SpreadsheetDataset
import com.example.model.TableDensity
import com.example.storage.TabloraStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class TabloraUiState(
    val currentScreen: AppScreen = AppScreen.DASHBOARD,
    val currentDataset: SpreadsheetDataset? = null,
    val originalDataset: SpreadsheetDataset? = null,
    val qualityReport: DataQualityReport? = null,
    val undoStack: List<SpreadsheetDataset> = emptyList(),
    val redoStack: List<SpreadsheetDataset> = emptyList(),
    val appliedOperations: List<CleaningOperation> = emptyList(),
    val presets: List<CleaningPreset> = emptyList(),
    val history: List<JobHistoryItem> = emptyList(),
    val density: TableDensity = TableDensity.NORMAL,
    val defaultExportFormat: ExportFormat = ExportFormat.CSV,
    val defaultCsvDelimiter: CsvDelimiter = CsvDelimiter.COMMA,
    // Table controls
    val searchQuery: String = "",
    val sortColumn: String? = null,
    val sortAscending: Boolean = true,
    val hiddenColumns: Set<String> = emptySet(),
    val currentPage: Int = 0,
    val pageSize: Int = 20,
    // Modals / Dialogs
    val activeDiffPreview: OperationDiff? = null,
    val showAskAiSheet: Boolean = false,
    val aiProposedPlan: AiProposedPlan? = null,
    val aiIsProcessing: Boolean = false,
    val toastMessage: String? = null,
    val isLoading: Boolean = false,
    val lastExportedFile: File? = null
)

class TabloraViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = TabloraStorage(application)

    private val _uiState = MutableStateFlow(TabloraUiState())
    val uiState: StateFlow<TabloraUiState> = _uiState.asStateFlow()

    init {
        loadInitialStorage()
        // Auto-load sample dataset for instant out-of-the-box delight
        loadSampleData(0)
    }

    private fun loadInitialStorage() {
        val presets = storage.getPresets()
        val history = storage.getHistory()
        val density = storage.getTableDensity()
        val exportFormat = storage.getDefaultExportFormat()
        val delimiter = storage.getDefaultCsvDelimiter()

        _uiState.update {
            it.copy(
                presets = presets,
                history = history,
                density = density,
                defaultExportFormat = exportFormat,
                defaultCsvDelimiter = delimiter
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    // -------------------------------------------------------------
    // DATASET LOADING
    // -------------------------------------------------------------

    fun loadSampleData(sampleIndex: Int) {
        val sample = when (sampleIndex) {
            0 -> SampleDataRepository.SAMPLE_CUSTOMERS
            1 -> SampleDataRepository.SAMPLE_SALES
            else -> SampleDataRepository.SAMPLE_INVENTORY
        }
        setNewDataset(sample)
    }

    fun loadFileFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val contentResolver = context.contentResolver
                val filename = getFilenameFromUri(context, uri)
                val isXlsx = filename.endsWith(".xlsx", ignoreCase = true) || filename.endsWith(".xls", ignoreCase = true)

                val dataset = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        if (isXlsx) {
                            SpreadsheetEngine.parseXlsx(stream, filename)
                        } else {
                            val delimiter = _uiState.value.defaultCsvDelimiter.char
                            SpreadsheetEngine.parseCsv(stream, filename, customDelimiter = delimiter)
                        }
                    }
                }

                if (dataset != null) {
                    setNewDataset(dataset)
                    _uiState.update {
                        it.copy(
                            currentScreen = AppScreen.WORKSPACE,
                            toastMessage = "Loaded ${dataset.rowCount} rows from $filename",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, toastMessage = "Unable to read spreadsheet file.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, toastMessage = "Import failed: ${e.message}") }
            }
        }
    }

    private fun setNewDataset(dataset: SpreadsheetDataset) {
        val report = DataQualityAnalyzer.analyze(dataset)
        _uiState.update {
            it.copy(
                currentDataset = dataset,
                originalDataset = dataset,
                qualityReport = report,
                undoStack = emptyList(),
                redoStack = emptyList(),
                appliedOperations = emptyList(),
                hiddenColumns = emptySet(),
                searchQuery = "",
                sortColumn = null,
                currentPage = 0
            )
        }
    }

    // -------------------------------------------------------------
    // CLEANING OPERATIONS & UNDO / REDO
    // -------------------------------------------------------------

    fun previewOperation(operation: CleaningOperation) {
        val current = _uiState.value.currentDataset ?: return
        val diff = CleaningEngine.preview(current, operation)
        _uiState.update { it.copy(activeDiffPreview = diff) }
    }

    fun dismissDiffPreview() {
        _uiState.update { it.copy(activeDiffPreview = null) }
    }

    fun applyOperation(operation: CleaningOperation) {
        val current = _uiState.value.currentDataset ?: return
        val newDataset = CleaningEngine.apply(current, operation)
        val newReport = DataQualityAnalyzer.analyze(newDataset)

        _uiState.update { state ->
            val newUndo = state.undoStack + current
            val newApplied = state.appliedOperations + operation
            state.copy(
                currentDataset = newDataset,
                qualityReport = newReport,
                undoStack = newUndo,
                redoStack = emptyList(), // Clear redo after new branch
                appliedOperations = newApplied,
                activeDiffPreview = null,
                toastMessage = "Applied: ${operation.displayName}"
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        val current = state.currentDataset ?: return
        if (state.undoStack.isEmpty()) return

        val previous = state.undoStack.last()
        val remainingUndo = state.undoStack.dropLast(1)
        val newRedo = listOf(current) + state.redoStack
        val newReport = DataQualityAnalyzer.analyze(previous)
        val newApplied = if (state.appliedOperations.isNotEmpty()) state.appliedOperations.dropLast(1) else emptyList()

        _uiState.update {
            it.copy(
                currentDataset = previous,
                qualityReport = newReport,
                undoStack = remainingUndo,
                redoStack = newRedo,
                appliedOperations = newApplied,
                toastMessage = "Undid last operation"
            )
        }
    }

    fun redo() {
        val state = _uiState.value
        val current = state.currentDataset ?: return
        if (state.redoStack.isEmpty()) return

        val next = state.redoStack.first()
        val remainingRedo = state.redoStack.drop(1)
        val newUndo = state.undoStack + current
        val newReport = DataQualityAnalyzer.analyze(next)

        _uiState.update {
            it.copy(
                currentDataset = next,
                qualityReport = newReport,
                undoStack = newUndo,
                redoStack = remainingRedo,
                toastMessage = "Redid operation"
            )
        }
    }

    fun resetToOriginal() {
        val original = _uiState.value.originalDataset ?: return
        val current = _uiState.value.currentDataset ?: return
        val report = DataQualityAnalyzer.analyze(original)

        _uiState.update {
            it.copy(
                currentDataset = original,
                qualityReport = report,
                undoStack = it.undoStack + current,
                redoStack = emptyList(),
                appliedOperations = emptyList(),
                toastMessage = "Dataset reset to original"
            )
        }
    }

    // -------------------------------------------------------------
    // PRESETS
    // -------------------------------------------------------------

    fun saveCurrentOperationsAsPreset(name: String, description: String) {
        val ops = _uiState.value.appliedOperations
        if (ops.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "No operations applied yet to save as preset.") }
            return
        }

        val preset = CleaningPreset(
            id = "preset_${UUID.randomUUID().toString().take(8)}",
            name = name.ifBlank { "Custom Preset (${ops.size} steps)" },
            description = description.ifBlank { "Applies ${ops.size} transformations sequentially." },
            operations = ops,
            isBuiltIn = false
        )

        storage.savePreset(preset)
        _uiState.update {
            it.copy(
                presets = storage.getPresets(),
                toastMessage = "Preset '${preset.name}' saved locally!"
            )
        }
    }

    fun applyPreset(preset: CleaningPreset) {
        val current = _uiState.value.currentDataset ?: return
        var dataset = current
        for (op in preset.operations) {
            dataset = CleaningEngine.apply(dataset, op)
        }
        val report = DataQualityAnalyzer.analyze(dataset)

        _uiState.update { state ->
            state.copy(
                currentDataset = dataset,
                qualityReport = report,
                undoStack = state.undoStack + current,
                redoStack = emptyList(),
                appliedOperations = state.appliedOperations + preset.operations,
                toastMessage = "Executed preset '${preset.name}' (${preset.operations.size} steps)"
            )
        }
    }

    fun deletePreset(presetId: String) {
        storage.deletePreset(presetId)
        _uiState.update { it.copy(presets = storage.getPresets(), toastMessage = "Preset deleted.") }
    }

    fun resetPresets() {
        storage.resetPresetsToDefault()
        _uiState.update { it.copy(presets = storage.getPresets(), toastMessage = "Presets reset to defaults.") }
    }

    // -------------------------------------------------------------
    // ASK TABLORA (AI CLEANING ASSISTANT)
    // -------------------------------------------------------------

    fun openAskAi() {
        _uiState.update { it.copy(showAskAiSheet = true, aiProposedPlan = null) }
    }

    fun closeAskAi() {
        _uiState.update { it.copy(showAskAiSheet = false, aiProposedPlan = null) }
    }

    fun submitAiPrompt(prompt: String) {
        val dataset = _uiState.value.currentDataset ?: return
        _uiState.update { it.copy(aiIsProcessing = true) }

        viewModelScope.launch {
            // Privacy safe: generate plan locally or via Gemini metadata
            val plan = withContext(Dispatchers.Default) {
                AiCleaningAssistant.generatePlan(prompt, dataset)
            }
            _uiState.update {
                it.copy(
                    aiProposedPlan = plan,
                    aiIsProcessing = false
                )
            }
        }
    }

    fun applyAiPlan(plan: AiProposedPlan) {
        val current = _uiState.value.currentDataset ?: return
        var dataset = current
        for (op in plan.operations) {
            dataset = CleaningEngine.apply(dataset, op)
        }
        val report = DataQualityAnalyzer.analyze(dataset)

        _uiState.update { state ->
            state.copy(
                currentDataset = dataset,
                qualityReport = report,
                undoStack = state.undoStack + current,
                redoStack = emptyList(),
                appliedOperations = state.appliedOperations + plan.operations,
                showAskAiSheet = false,
                aiProposedPlan = null,
                toastMessage = "Applied AI Plan (${plan.operations.size} operations)"
            )
        }
    }

    // -------------------------------------------------------------
    // EXPORT
    // -------------------------------------------------------------

    fun exportDataset(
        context: Context,
        format: ExportFormat,
        filenameWithoutExt: String,
        selectedColumns: List<String>
    ): File? {
        val dataset = _uiState.value.currentDataset ?: return null
        val baseName = filenameWithoutExt.ifBlank { dataset.name.substringBeforeLast(".") }
        val filename = "$baseName.${format.extension}"

        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val outFile = File(exportDir, filename)

            FileOutputStream(outFile).use { fos ->
                if (format == ExportFormat.XLSX) {
                    val bytes = SpreadsheetEngine.exportToXlsx(dataset, selectedColumns)
                    fos.write(bytes)
                } else {
                    val delimiter = _uiState.value.defaultCsvDelimiter.char
                    val csvText = SpreadsheetEngine.exportToCsv(dataset, delimiter, selectedColumns)
                    fos.write(csvText.toByteArray(Charsets.UTF_8))
                }
            }

            // Record in Job History
            val historyItem = JobHistoryItem(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                filename = filename,
                originalRows = dataset.originalRowCount,
                originalCols = dataset.originalColumnCount,
                finalRows = dataset.rowCount,
                finalCols = selectedColumns.size,
                operationsCount = _uiState.value.appliedOperations.size,
                exportFormat = format.name,
                completedExport = true
            )
            storage.addHistoryItem(historyItem)

            _uiState.update {
                it.copy(
                    history = storage.getHistory(),
                    lastExportedFile = outFile,
                    toastMessage = "Exported $filename successfully!"
                )
            }
            outFile
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Export failed: ${e.message}") }
            null
        }
    }

    // -------------------------------------------------------------
    // TABLE CONTROLS
    // -------------------------------------------------------------

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, currentPage = 0) }
    }

    fun setSortColumn(column: String) {
        _uiState.update { state ->
            val asc = if (state.sortColumn == column) !state.sortAscending else true
            state.copy(sortColumn = column, sortAscending = asc, currentPage = 0)
        }
    }

    fun toggleColumnVisibility(column: String) {
        _uiState.update { state ->
            val set = state.hiddenColumns.toMutableSet()
            if (set.contains(column)) set.remove(column) else set.add(column)
            state.copy(hiddenColumns = set)
        }
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceAtLeast(0)) }
    }

    fun setDensity(density: TableDensity) {
        storage.setTableDensity(density)
        _uiState.update { it.copy(density = density) }
    }

    fun setDefaultExportFormat(format: ExportFormat) {
        storage.setDefaultExportFormat(format)
        _uiState.update { it.copy(defaultExportFormat = format) }
    }

    fun setDefaultCsvDelimiter(delimiter: CsvDelimiter) {
        storage.setDefaultCsvDelimiter(delimiter)
        _uiState.update { it.copy(defaultCsvDelimiter = delimiter) }
    }

    fun clearHistory() {
        storage.clearHistory()
        _uiState.update { it.copy(history = emptyList(), toastMessage = "History cleared.") }
    }

    fun deleteHistoryItem(id: String) {
        storage.deleteHistoryItem(id)
        _uiState.update { it.copy(history = storage.getHistory(), toastMessage = "Record deleted.") }
    }

    fun clearAllData() {
        storage.clearAllData()
        loadInitialStorage()
        _uiState.update { it.copy(toastMessage = "All local data and preferences reset.") }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun getFilenameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "spreadsheet.csv"
    }
}
