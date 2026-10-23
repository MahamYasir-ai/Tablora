package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.model.AiProposedPlan
import com.example.model.ExportFormat
import com.example.model.OperationDiff
import com.example.model.QualitySeverity
import com.example.ui.theme.TabloraAmber
import com.example.ui.theme.TabloraAmberLight
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraEmerald
import com.example.ui.theme.TabloraEmeraldLight
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraRose
import com.example.ui.theme.TabloraRoseLight
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

/**
 * Top application header with brand logo, tagline, quick undo/redo, and Ask Tablora action.
 */
@Composable
fun TabloraHeader(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onAskAi: () -> Unit,
    onExport: () -> Unit,
    hasDataset: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TabloraNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Tablora Logo",
                        tint = TabloraSky,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tablora",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TabloraNavy
                        )
                    )
                    Text(
                        text = "Messy data in. Clean data out.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TabloraSlateLight,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasDataset) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) TabloraSlate else TabloraSlateLight.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) TabloraSlate else TabloraSlateLight.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset to Original",
                            tint = TabloraSlate
                        )
                    }

                    Button(
                        onClick = onAskAi,
                        colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("ask_tablora_header_btn")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Tablora", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.testTag("export_header_btn")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export Clean Dataset",
                            tint = TabloraEmerald
                        )
                    }
                }
            }
        }
    }
}

/**
 * Severity tag indicator.
 */
@Composable
fun SeverityChip(severity: QualitySeverity) {
    val (bg, fg, label) = when (severity) {
        QualitySeverity.ERROR -> Triple(TabloraRoseLight, TabloraRose, "Definite Error")
        QualitySeverity.WARNING -> Triple(TabloraAmberLight, TabloraAmber, "Heuristic Warning")
        QualitySeverity.INFO -> Triple(Color(0xFFE2E8F0), TabloraSlate, "Info")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Operation diff preview modal before committing changes.
 */
@Composable
fun OperationPreviewDialog(
    diff: OperationDiff,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Preview: ${diff.operation.displayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TabloraNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = diff.operation.description,
                    fontSize = 12.sp,
                    color = TabloraSlateLight
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Summary stat cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Rows", fontSize = 11.sp, color = TabloraSlateLight)
                            Text(
                                "${diff.rowsBefore} → ${diff.rowsAfter}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TabloraNavy
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Columns", fontSize = 11.sp, color = TabloraSlateLight)
                            Text(
                                "${diff.columnsBefore} → ${diff.columnsAfter}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TabloraNavy
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Modified", fontSize = 11.sp, color = TabloraSlateLight)
                            Text(
                                "${diff.modifiedCellsCount} cells",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TabloraSky
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = diff.summaryMessage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TabloraEmerald
                )

                if (diff.previewRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sample Result (${diff.previewRows.size} rows):",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TabloraSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        items(diff.previewRows) { row ->
                            Text(
                                text = row.joinToString(" | ") { it.ifEmpty { "•" } },
                                fontSize = 11.sp,
                                color = TabloraSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_apply_op_btn")
            ) {
                Text("Confirm & Apply")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Ask Tablora AI Assistant modal sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskTabloraBottomSheet(
    onDismiss: () -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onApplyPlan: (AiProposedPlan) -> Unit,
    proposedPlan: AiProposedPlan?,
    isProcessing: Boolean
) {
    var promptInput by remember { mutableStateOf("") }

    val sampleSuggestions = listOf(
        "Remove empty rows and duplicate customers by email",
        "Trim spaces across all text and normalize dates to ISO",
        "Strip currency symbols and convert numbers",
        "Identify and lowercase email addresses",
        "Suggest a complete cleaning workflow for this dataset"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TabloraSky),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ask Tablora",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TabloraNavy
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Describe your data cleaning objective in plain English. Tablora proposes a verified plan that you can inspect and approve.",
                fontSize = 12.sp,
                color = TabloraSlateLight
            )

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = { Text("What would you like to clean?") },
                placeholder = { Text("e.g. Deduplicate by email and standardize all dates") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_prompt_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("Quick Suggestions:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TabloraSlate)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sampleSuggestions.take(3).forEach { suggestion ->
                    Text(
                        text = "• $suggestion",
                        fontSize = 11.sp,
                        color = TabloraSky,
                        modifier = Modifier
                            .clickable { promptInput = suggestion }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { if (promptInput.isNotBlank()) onSubmitPrompt(promptInput) },
                enabled = promptInput.isNotBlank() && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_ai_prompt_btn")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing schema...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Cleaning Plan")
                }
            }

            // Proposed Plan section
            if (proposedPlan != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Proposed Plan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TabloraNavy
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TabloraEmeraldLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${proposedPlan.operations.size} steps",
                                    color = TabloraEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = proposedPlan.rationale,
                            fontSize = 12.sp,
                            color = TabloraSlate
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onApplyPlan(proposedPlan) },
                            colors = ButtonDefaults.buttonColors(containerColor = TabloraEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_ai_plan_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Approve & Apply Plan")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Export dialog supporting CSV and XLSX.
 */
@Composable
fun ExportCenterDialog(
    currentFilename: String,
    allColumns: List<String>,
    defaultFormat: ExportFormat,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, String, List<String>) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(defaultFormat) }
    var filename by remember { mutableStateOf(currentFilename.substringBeforeLast(".")) }
    var selectedCols by remember { mutableStateOf(allColumns.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export Clean Dataset", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TabloraNavy)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Output Filename") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Format:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TabloraSlate)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedFormat = ExportFormat.CSV }
                    ) {
                        RadioButton(
                            selected = selectedFormat == ExportFormat.CSV,
                            onClick = { selectedFormat = ExportFormat.CSV }
                        )
                        Text("CSV", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedFormat = ExportFormat.XLSX }
                    ) {
                        RadioButton(
                            selected = selectedFormat == ExportFormat.XLSX,
                            onClick = { selectedFormat = ExportFormat.XLSX }
                        )
                        Text("Excel (.xlsx)", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Columns to Include (${selectedCols.size}/${allColumns.size}):", fontSize = 12.sp, color = TabloraSlateLight)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, TabloraBorder, RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    items(allColumns) { col ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedCols.toMutableSet()
                                    if (newSet.contains(col)) newSet.remove(col) else newSet.add(col)
                                    selectedCols = newSet
                                }
                        ) {
                            Checkbox(
                                checked = selectedCols.contains(col),
                                onCheckedChange = { checked ->
                                    val newSet = selectedCols.toMutableSet()
                                    if (checked) newSet.add(col) else newSet.remove(col)
                                    selectedCols = newSet
                                }
                            )
                            Text(col, fontSize = 12.sp, color = TabloraSlate)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat, filename, selectedCols.toList()) },
                enabled = selectedCols.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TabloraEmerald),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_export_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
