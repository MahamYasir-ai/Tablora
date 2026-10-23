package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.CsvDelimiter
import com.example.model.ExportFormat
import com.example.model.TableDensity
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraRose
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSkyLight
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentDensity: TableDensity,
    defaultExportFormat: ExportFormat,
    defaultCsvDelimiter: CsvDelimiter,
    onDensityChange: (TableDensity) -> Unit,
    onExportFormatChange: (ExportFormat) -> Unit,
    onCsvDelimiterChange: (CsvDelimiter) -> Unit,
    onResetPresets: () -> Unit,
    onClearHistory: () -> Unit,
    onClearAllData: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmWipeDialog by remember { mutableStateOf(false) }
    var delimiterMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & Privacy",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TabloraNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Configure table display, default export formats, and manage local storage.",
                fontSize = 12.sp,
                color = TabloraSlateLight
            )
        }

        // Table Density Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Table Row Density", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TabloraNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Controls the row height and compactness of spreadsheet cells in the workspace.", fontSize = 12.sp, color = TabloraSlateLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TableDensity.values().forEach { density ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (currentDensity == density) TabloraSkyLight else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        if (currentDensity == density) TabloraSky else TabloraBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDensityChange(density) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = density.label,
                                    fontWeight = if (currentDensity == density) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (currentDensity == density) TabloraSky else TabloraSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Defaults
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export & Serialization Defaults", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TabloraNavy)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Default File Format:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TabloraSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onExportFormatChange(ExportFormat.CSV) }) {
                            RadioButton(selected = defaultExportFormat == ExportFormat.CSV, onClick = { onExportFormatChange(ExportFormat.CSV) })
                            Text("CSV (.csv)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onExportFormatChange(ExportFormat.XLSX) }) {
                            RadioButton(selected = defaultExportFormat == ExportFormat.XLSX, onClick = { onExportFormatChange(ExportFormat.XLSX) })
                            Text("Excel (.xlsx)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Default CSV Delimiter:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TabloraSlate)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = delimiterMenuExpanded,
                        onExpandedChange = { delimiterMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = defaultCsvDelimiter.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = delimiterMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = delimiterMenuExpanded,
                            onDismissRequest = { delimiterMenuExpanded = false }
                        ) {
                            CsvDelimiter.values().forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.label) },
                                    onClick = {
                                        onCsvDelimiterChange(d)
                                        delimiterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Privacy & Architecture Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TabloraSky, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Security Architecture", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TabloraNavy)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• On-Device Core: 100% of spreadsheet parsing, deduplication, cleaning operations, and file exports execute locally in client memory.\n" +
                                "• Local Persistence: Preferences, cleaning presets, and job metadata are saved to your device's local storage and never automatically synced.\n" +
                                "• AI Protection: The 'Ask Tablora' assistant communicates column metadata only upon explicit user invocation. Raw cell rows are never silently transmitted to external servers.\n" +
                                "• Zero Account Requirement: No account, login, or tracking cookies are required.",
                        fontSize = 11.sp,
                        color = TabloraSlate,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Local Storage Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Data Management", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TabloraNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Manage cached preferences, presets, and history records.", fontSize = 12.sp, color = TabloraSlateLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onResetPresets,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Presets", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onClearHistory,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear History", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showConfirmWipeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TabloraRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wipe_all_data_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All Application Data", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showConfirmWipeDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmWipeDialog = false },
            title = { Text("Reset All Local Data?", fontWeight = FontWeight.Bold, color = TabloraNavy) },
            text = { Text("This will delete all custom presets, job history, and reset your preferences to default. This cannot be undone.", fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showConfirmWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TabloraRose)
                ) {
                    Text("Yes, Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmWipeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
