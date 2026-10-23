package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.CleaningOperation
import com.example.model.DateOutputFormat
import com.example.model.EmailAction
import com.example.model.FillStrategy
import com.example.model.FilterOperator
import com.example.model.NumericInvalidHandling
import com.example.model.PhoneAction
import com.example.model.SpreadsheetDataset
import com.example.model.TableDensity
import com.example.model.TextCase
import com.example.ui.theme.TabloraAmber
import com.example.ui.theme.TabloraAmberLight
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraEmerald
import com.example.ui.theme.TabloraEmeraldLight
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSkyLight
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    dataset: SpreadsheetDataset?,
    appliedOperations: List<CleaningOperation>,
    canUndo: Boolean,
    canRedo: Boolean,
    density: TableDensity,
    searchQuery: String,
    sortColumn: String?,
    sortAscending: Boolean,
    hiddenColumns: Set<String>,
    currentPage: Int,
    pageSize: Int,
    onSearchChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onToggleColumnVisibility: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onPreviewOperation: (CleaningOperation) -> Unit,
    onApplyOperation: (CleaningOperation) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onOpenAskAi: () -> Unit,
    onOpenExport: () -> Unit,
    onSaveAsPreset: (String, String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dataset == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No dataset loaded in workspace", fontWeight = FontWeight.Bold, color = TabloraNavy)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onNavigate(AppScreen.NEW_JOB) },
                    colors = ButtonDefaults.buttonColors(containerColor = TabloraSky)
                ) {
                    Text("Select or Upload Dataset")
                }
            }
        }
        return
    }

    // Modal dialog states for operations
    var showOperationsMenu by remember { mutableStateOf(false) }
    var showColumnsMenu by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var presetDescInput by remember { mutableStateOf("") }

    // Operation configuration dialog states
    var activeOpDialog by remember { mutableStateOf<String?>(null) }
    var selectedTargetCol by remember { mutableStateOf(dataset.columns.firstOrNull().orEmpty()) }
    var textCaseChoice by remember { mutableStateOf(TextCase.LOWER) }
    var dateFmtChoice by remember { mutableStateOf(DateOutputFormat.ISO_YMD) }
    var findTextInput by remember { mutableStateOf("") }
    var replaceTextInput by remember { mutableStateOf("") }
    var renameColInput by remember { mutableStateOf("") }
    var filterValueInput by remember { mutableStateOf("") }
    var filterOpChoice by remember { mutableStateOf(FilterOperator.EQUALS) }
    var emailActionChoice by remember { mutableStateOf(EmailAction.TRIM_AND_LOWER) }
    var phoneActionChoice by remember { mutableStateOf(PhoneAction.STANDARD_US) }
    var fillStrategyChoice by remember { mutableStateOf(FillStrategy.PLACEHOLDER_NA) }
    var fillCustomInput by remember { mutableStateOf("") }

    // Computed filtered and sorted rows
    val visibleColumns = remember(dataset.columns, hiddenColumns) {
        dataset.columns.filter { it !in hiddenColumns }
    }

    val filteredRows by remember(dataset.rows, searchQuery, sortColumn, sortAscending, dataset.columns) {
        derivedStateOf {
            var rows = dataset.rows
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                rows = rows.filter { row -> row.any { it.lowercase().contains(q) } }
            }
            if (sortColumn != null) {
                val colIdx = dataset.columns.indexOf(sortColumn)
                if (colIdx != -1) {
                    rows = rows.sortedWith { r1, r2 ->
                        val v1 = r1.getOrElse(colIdx) { "" }
                        val v2 = r2.getOrElse(colIdx) { "" }
                        val n1 = v1.toDoubleOrNull()
                        val n2 = v2.toDoubleOrNull()
                        val comparison = if (n1 != null && n2 != null) n1.compareTo(n2) else v1.compareTo(v2, ignoreCase = true)
                        if (sortAscending) comparison else -comparison
                    }
                }
            }
            rows
        }
    }

    val totalPages = (filteredRows.size + pageSize - 1) / pageSize.coerceAtLeast(1)
    val pagedRows = remember(filteredRows, currentPage, pageSize) {
        val start = (currentPage * pageSize).coerceIn(0, filteredRows.size)
        val end = (start + pageSize).coerceIn(start, filteredRows.size)
        filteredRows.subList(start, end)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Workspace Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Top row with search and primary action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search rows...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("workspace_search_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Operations button
                    Box {
                        Button(
                            onClick = { showOperationsMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TabloraNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("clean_operations_btn")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clean Tools", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showOperationsMenu,
                            onDismissRequest = { showOperationsMenu = false }
                        ) {
                            Text("Quick Clean Operations", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TabloraSlateLight, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            DropdownMenuItem(text = { Text("1. Remove Empty Rows") }, onClick = { showOperationsMenu = false; onPreviewOperation(CleaningOperation.RemoveEmptyRows) })
                            DropdownMenuItem(text = { Text("2. Remove Empty Columns") }, onClick = { showOperationsMenu = false; onPreviewOperation(CleaningOperation.RemoveEmptyColumns) })
                            DropdownMenuItem(text = { Text("3. Remove Exact Duplicate Rows") }, onClick = { showOperationsMenu = false; onPreviewOperation(CleaningOperation.RemoveExactDuplicates) })
                            DropdownMenuItem(text = { Text("4. Deduplicate by Column...") }, onClick = { showOperationsMenu = false; activeOpDialog = "dedup_col" })
                            DropdownMenuItem(text = { Text("5. Trim Whitespace (All Text)") }, onClick = { showOperationsMenu = false; onPreviewOperation(CleaningOperation.TrimWhitespace(dataset.columns, trimAllEligible = true)) })
                            DropdownMenuItem(text = { Text("6. Normalize Internal Spaces...") }, onClick = { showOperationsMenu = false; activeOpDialog = "internal_spaces" })
                            DropdownMenuItem(text = { Text("7. Convert Case (Lower/Upper/Title)...") }, onClick = { showOperationsMenu = false; activeOpDialog = "convert_case" })
                            DropdownMenuItem(text = { Text("8. Normalize Blanks / Nulls") }, onClick = { showOperationsMenu = false; onPreviewOperation(CleaningOperation.NormalizeBlanks(dataset.columns)) })
                            DropdownMenuItem(text = { Text("9. Clean & Convert to Numeric...") }, onClick = { showOperationsMenu = false; activeOpDialog = "numeric" })
                            DropdownMenuItem(text = { Text("10. Standardize Dates (ISO/US/EU)...") }, onClick = { showOperationsMenu = false; activeOpDialog = "dates" })
                            DropdownMenuItem(text = { Text("11. Find and Replace...") }, onClick = { showOperationsMenu = false; activeOpDialog = "find_replace" })
                            DropdownMenuItem(text = { Text("12. Rename Column...") }, onClick = { showOperationsMenu = false; activeOpDialog = "rename_col" })
                            DropdownMenuItem(text = { Text("13. Remove Selected Columns...") }, onClick = { showOperationsMenu = false; activeOpDialog = "remove_col" })
                            DropdownMenuItem(text = { Text("14. Filter Rows by Condition...") }, onClick = { showOperationsMenu = false; activeOpDialog = "filter_rows" })
                            DropdownMenuItem(text = { Text("15. Validate Email Addresses...") }, onClick = { showOperationsMenu = false; activeOpDialog = "email" })
                            DropdownMenuItem(text = { Text("16. Standardize Phone Numbers...") }, onClick = { showOperationsMenu = false; activeOpDialog = "phone" })
                            DropdownMenuItem(text = { Text("17. Fill Missing Values...") }, onClick = { showOperationsMenu = false; activeOpDialog = "fill_missing" })
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Column Visibility Dropdown
                    Box {
                        IconButton(onClick = { showColumnsMenu = true }) {
                            Icon(Icons.Default.ViewColumn, contentDescription = "Columns Visibility", tint = TabloraSlate)
                        }
                        DropdownMenu(
                            expanded = showColumnsMenu,
                            onDismissRequest = { showColumnsMenu = false }
                        ) {
                            Text("Toggle Columns", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TabloraSlateLight, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            dataset.columns.forEach { col ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = col in visibleColumns,
                                                onCheckedChange = { onToggleColumnVisibility(col) }
                                            )
                                            Text(col, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = { onToggleColumnVisibility(col) }
                                )
                            }
                        }
                    }

                    // Save as preset action
                    IconButton(
                        onClick = { showSavePresetDialog = true },
                        enabled = appliedOperations.isNotEmpty(),
                        modifier = Modifier.testTag("save_as_preset_btn")
                    ) {
                        Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = "Save as Preset",
                            tint = if (appliedOperations.isNotEmpty()) TabloraSky else TabloraSlateLight.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats and Applied Operations Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${filteredRows.size} of ${dataset.rowCount} rows",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TabloraNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${visibleColumns.size}/${dataset.columnCount} cols)",
                            fontSize = 11.sp,
                            color = TabloraSlateLight
                        )
                    }

                    if (appliedOperations.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TabloraEmeraldLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${appliedOperations.size} operations applied",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TabloraEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spreadsheet Data Table
        val horizontalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder))
                ) {
                    // Row # Index Header
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(density.rowHeightDp.dp)
                            .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TabloraSlateLight)
                    }

                    // Column Headers
                    visibleColumns.forEach { colName ->
                        val isSorted = sortColumn == colName
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(density.rowHeightDp.dp)
                                .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder))
                                .clickable { onSortChange(colName) }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = colName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSorted) TabloraSky else TabloraNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSorted) {
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Sorted",
                                        tint = TabloraSky,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Table Data Rows (Virtualized with LazyColumn)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(pagedRows) { index, row ->
                        val globalRowIndex = (currentPage * pageSize) + index + 1
                        val isEven = index % 2 == 0

                        Row(
                            modifier = Modifier
                                .background(if (isEven) Color.White else Color(0xFFFBFDFF))
                                .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder))
                        ) {
                            // Row Number Cell
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(density.rowHeightDp.dp)
                                    .background(Color(0xFFF8FAFC))
                                    .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$globalRowIndex",
                                    fontSize = 10.sp,
                                    color = TabloraSlateLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Data Cells
                            visibleColumns.forEach { colName ->
                                val colIdx = dataset.columns.indexOf(colName)
                                val cellVal = if (colIdx in row.indices) row[colIdx] else ""
                                val isBlank = cellVal.isBlank()

                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(density.rowHeightDp.dp)
                                        .border(androidx.compose.foundation.BorderStroke(0.5.dp, TabloraBorder))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (isBlank) {
                                        Text(
                                            text = "•",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Text(
                                            text = cellVal,
                                            fontSize = 11.sp,
                                            color = TabloraSlate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pagination Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Page ${if (totalPages == 0) 0 else currentPage + 1} of ${maxOf(1, totalPages)}",
                    fontSize = 12.sp,
                    color = TabloraSlateLight
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onPageChange(currentPage - 1) },
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("prev_page_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page")
                    }

                    IconButton(
                        onClick = { onPageChange(currentPage + 1) },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.testTag("next_page_btn")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                    }
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Cleaning Preset", fontWeight = FontWeight.Bold, color = TabloraNavy) },
            text = {
                Column {
                    Text(
                        "Package the currently applied ${appliedOperations.size} operation(s) into a reusable preset that persists locally.",
                        fontSize = 12.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") },
                        placeholder = { Text("e.g. Clean Customer List") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = presetDescInput,
                        onValueChange = { presetDescInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAsPreset(presetNameInput, presetDescInput)
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TabloraSky)
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Config Dialogs for Specific Operations
    when (activeOpDialog) {
        "dedup_col" -> {
            var selectedCols by remember { mutableStateOf(setOf(dataset.columns.firstOrNull().orEmpty())) }
            AlertDialog(
                onDismissRequest = { activeOpDialog = null },
                title = { Text("Deduplicate by Column(s)") },
                text = {
                    Column {
                        Text("Select columns that define a duplicate record:", fontSize = 12.sp)
                        dataset.columns.forEach { col ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                val set = selectedCols.toMutableSet()
                                if (set.contains(col)) set.remove(col) else set.add(col)
                                selectedCols = set
                            }) {
                                Checkbox(checked = col in selectedCols, onCheckedChange = { chk ->
                                    val set = selectedCols.toMutableSet()
                                    if (chk) set.add(col) else set.remove(col)
                                    selectedCols = set
                                })
                                Text(col, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        activeOpDialog = null
                        onPreviewOperation(CleaningOperation.RemoveDuplicatesByColumns(selectedCols.toList()))
                    }) { Text("Preview") }
                },
                dismissButton = { TextButton(onClick = { activeOpDialog = null }) { Text("Cancel") } }
            )
        }

        "convert_case" -> {
            AlertDialog(
                onDismissRequest = { activeOpDialog = null },
                title = { Text("Convert Text Case") },
                text = {
                    Column {
                        Text("Target Case:", fontSize = 12.sp)
                        TextCase.values().forEach { tc ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { textCaseChoice = tc }) {
                                androidx.compose.material3.RadioButton(selected = textCaseChoice == tc, onClick = { textCaseChoice = tc })
                                Text(tc.label, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        activeOpDialog = null
                        onPreviewOperation(CleaningOperation.ConvertCase(dataset.columns, textCaseChoice))
                    }) { Text("Preview") }
                },
                dismissButton = { TextButton(onClick = { activeOpDialog = null }) { Text("Cancel") } }
            )
        }

        "dates" -> {
            AlertDialog(
                onDismissRequest = { activeOpDialog = null },
                title = { Text("Standardize Dates") },
                text = {
                    Column {
                        Text("Target Format:", fontSize = 12.sp)
                        DateOutputFormat.values().forEach { fmt ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dateFmtChoice = fmt }) {
                                androidx.compose.material3.RadioButton(selected = dateFmtChoice == fmt, onClick = { dateFmtChoice = fmt })
                                Text(fmt.label, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        activeOpDialog = null
                        onPreviewOperation(CleaningOperation.StandardizeDates(dataset.columns, dateFmtChoice))
                    }) { Text("Preview") }
                },
                dismissButton = { TextButton(onClick = { activeOpDialog = null }) { Text("Cancel") } }
            )
        }

        "find_replace" -> {
            AlertDialog(
                onDismissRequest = { activeOpDialog = null },
                title = { Text("Find & Replace") },
                text = {
                    Column {
                        OutlinedTextField(value = findTextInput, onValueChange = { findTextInput = it }, label = { Text("Find text") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = replaceTextInput, onValueChange = { replaceTextInput = it }, label = { Text("Replace with") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        activeOpDialog = null
                        onPreviewOperation(CleaningOperation.FindAndReplace(targetColumn = null, findText = findTextInput, replaceText = replaceTextInput))
                    }) { Text("Preview") }
                },
                dismissButton = { TextButton(onClick = { activeOpDialog = null }) { Text("Cancel") } }
            )
        }

        "numeric" -> {
            AlertDialog(
                onDismissRequest = { activeOpDialog = null },
                title = { Text("Convert to Numeric") },
                text = {
                    Text("Cleans currency symbols (\$, €, £), thousand commas, and parses valid decimal numbers across eligible columns.")
                },
                confirmButton = {
                    Button(onClick = {
                        activeOpDialog = null
                        onPreviewOperation(CleaningOperation.ConvertToNumeric(dataset.columns, stripCurrencyAndCommas = true))
                    }) { Text("Preview") }
                },
                dismissButton = { TextButton(onClick = { activeOpDialog = null }) { Text("Cancel") } }
            )
        }
    }
}
