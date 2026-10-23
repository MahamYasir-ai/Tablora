package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.CsvDelimiter
import com.example.model.SpreadsheetDataset
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
fun NewJobScreen(
    currentDataset: SpreadsheetDataset?,
    defaultDelimiter: CsvDelimiter,
    isLoading: Boolean,
    onLoadFile: (Uri) -> Unit,
    onLoadSample: (Int) -> Unit,
    onChangeDelimiter: (CsvDelimiter) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var delimiterMenuExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onLoadFile(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upload Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_dropzone_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(2.dp, TabloraSky.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TabloraSkyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = TabloraSky)
                        } else {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = TabloraSky,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Upload your spreadsheet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TabloraNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Drag & drop or tap to browse. Supports CSV, XLSX, and XLS up to 20 MB.",
                        fontSize = 12.sp,
                        color = TabloraSlateLight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { filePicker.launch("*/*") },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("browse_files_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Files on Device", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TabloraSlateLight, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Files are parsed strictly locally on your device. Zero cloud upload.",
                            fontSize = 11.sp,
                            color = TabloraSlateLight
                        )
                    }
                }
            }
        }

        // CSV Delimiter & Workbook Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Import Configurations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TabloraNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tablora auto-detects CSV delimiters and worksheet tables, but you can explicitly specify preferences below:",
                        fontSize = 12.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = delimiterMenuExpanded,
                        onExpandedChange = { delimiterMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = defaultDelimiter.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("CSV Delimiter") },
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
                                        onChangeDelimiter(d)
                                        delimiterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Current Dataset Preview Card
        if (currentDataset != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TabloraEmeraldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TabloraEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentDataset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TabloraNavy
                                    )
                                    Text(
                                        text = "${currentDataset.rowCount} rows • ${currentDataset.columnCount} columns • Sheet: ${currentDataset.sheetName}",
                                        fontSize = 11.sp,
                                        color = TabloraSlateLight
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TabloraSkyLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(currentDataset.sourceFormat, color = TabloraSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "First 5 Rows Preview:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TabloraSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Preview Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = currentDataset.columns.joinToString(" | "),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TabloraNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            currentDataset.rows.take(5).forEach { row ->
                                Text(
                                    text = row.joinToString(" | ") { it.ifEmpty { "•" } },
                                    fontSize = 11.sp,
                                    color = TabloraSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onNavigate(AppScreen.WORKSPACE) },
                                colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open in Workspace")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            OutlinedButton(
                                onClick = { onNavigate(AppScreen.QUALITY_REPORT) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Quality Report")
                            }
                        }
                    }
                }
            }
        }

        // Test with Fictional Sample Datasets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Or Test with Realistic Messy Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TabloraNavy
                    )
                    Text(
                        text = "Select a fictional business dataset containing messy phone numbers, missing emails, dirty numbers, and duplicate entries:",
                        fontSize = 12.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleRow(
                            title = "Ecommerce Customer Roster",
                            description = "15 rows • Mixed emails, unpadded spaces, leading zeros, duplicate records.",
                            onClick = {
                                onLoadSample(0)
                                onNavigate(AppScreen.WORKSPACE)
                            }
                        )

                        SampleRow(
                            title = "B2B Sales Pipeline",
                            description = "6 deals • Currency strings (\$45,000), proposal stages, mixed dates.",
                            onClick = {
                                onLoadSample(1)
                                onNavigate(AppScreen.WORKSPACE)
                            }
                        )

                        SampleRow(
                            title = "Warehouse SKU Inventory",
                            description = "5 items • SKU codes with leading zeros (004921), stock with invalid text.",
                            onClick = {
                                onLoadSample(2)
                                onNavigate(AppScreen.WORKSPACE)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, TabloraBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TabloraNavy)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = TabloraSlateLight)
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TabloraSky, modifier = Modifier.size(18.dp))
    }
}
