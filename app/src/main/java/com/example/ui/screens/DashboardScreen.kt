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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.CleaningPreset
import com.example.model.JobHistoryItem
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

@Composable
fun DashboardScreen(
    currentDataset: SpreadsheetDataset?,
    history: List<JobHistoryItem>,
    presets: List<CleaningPreset>,
    onNavigate: (AppScreen) -> Unit,
    onLoadSample: (Int) -> Unit,
    onApplyPreset: (CleaningPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    // True computed stats strictly from real job records — never fabricated!
    val totalJobs = history.size
    val totalFiles = history.map { it.filename }.distinct().size
    val totalRowsExamined = history.sumOf { it.originalRows }
    val totalOperations = history.sumOf { it.operationsCount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Welcome Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TabloraNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TabloraSky.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "B2B SPREADSHEET CLEANING",
                                color = TabloraSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        if (currentDataset != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TabloraEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Dataset Active: ${currentDataset.rowCount} rows",
                                    color = TabloraEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Messy data in.\nClean data out.",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clean, validate, transform, and export CSV and Excel files in seconds without writing code. 100% private, on-device processing.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onNavigate(AppScreen.NEW_JOB) },
                            colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_spreadsheet_cta")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload File", fontWeight = FontWeight.Bold)
                        }

                        if (currentDataset != null) {
                            OutlinedButton(
                                onClick = { onNavigate(AppScreen.WORKSPACE) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Workspace")
                            }
                        }
                    }
                }
            }
        }

        // Real Locally Computed Job Statistics
        item {
            Text(
                text = "Processing Activity",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TabloraNavy
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Files",
                    value = "$totalFiles",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Completed Jobs",
                    value = "$totalJobs",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Rows Processed",
                    value = "$totalRowsExamined",
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Operations",
                    value = "$totalOperations",
                    icon = Icons.Default.CleaningServices,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick-Start Fictional Sample Datasets
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
                        Text(
                            text = "Quick Start with Sample Data",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TabloraNavy
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TabloraAmberLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Fictional Test Data", color = TabloraAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instant real datasets with duplicates, whitespace issues, invalid emails, and unformatted dates to test Tablora operations:",
                        fontSize = 12.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SampleDatasetButton(
                            title = "Ecommerce Customers",
                            subtitle = "Duplicates & Emails",
                            onClick = {
                                onLoadSample(0)
                                onNavigate(AppScreen.WORKSPACE)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SampleDatasetButton(
                            title = "Sales Pipeline",
                            subtitle = "Currency & Dates",
                            onClick = {
                                onLoadSample(1)
                                onNavigate(AppScreen.WORKSPACE)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SampleDatasetButton(
                            title = "Inventory SKUs",
                            subtitle = "Zeros & Types",
                            onClick = {
                                onLoadSample(2)
                                onNavigate(AppScreen.WORKSPACE)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Messy vs Clean Comparison Example
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transformation Example",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TabloraNavy
                    )
                    Text(
                        text = "Demonstration with fictional sample data:",
                        fontSize = 11.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Messy Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF1F2))
                                .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("MESSY INPUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBE123C))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("•  Alice Cooper  (untrimmed)", fontSize = 11.sp, color = TabloraSlate)
                            Text("• john.doe@bad-email", fontSize = 11.sp, color = TabloraSlate)
                            Text("• $1,450.00 (raw string)", fontSize = 11.sp, color = TabloraSlate)
                            Text("• 03/22/2023 (mixed date)", fontSize = 11.sp, color = TabloraSlate)
                            Text("• Duplicate rows & blanks", fontSize = 11.sp, color = TabloraSlate)
                        }

                        // Clean Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TabloraEmeraldLight)
                                .border(1.dp, TabloraEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("CLEAN OUTPUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TabloraEmerald)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("✓ Alice Cooper (trimmed)", fontSize = 11.sp, color = TabloraSlate)
                            Text("✓ Validated & lowercased", fontSize = 11.sp, color = TabloraSlate)
                            Text("✓ 1450.00 (numeric float)", fontSize = 11.sp, color = TabloraSlate)
                            Text("✓ 2023-03-22 (ISO date)", fontSize = 11.sp, color = TabloraSlate)
                            Text("✓ Zero duplicates or blanks", fontSize = 11.sp, color = TabloraSlate)
                        }
                    }
                }
            }
        }

        // Quick Reusable Presets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Cleaning Presets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TabloraNavy
                )
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    color = TabloraSky,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppScreen.PRESETS) }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.take(3).forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (currentDataset != null) onApplyPreset(preset)
                                else onNavigate(AppScreen.PRESETS)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TabloraNavy
                                )
                                Text(
                                    text = preset.description,
                                    fontSize = 12.sp,
                                    color = TabloraSlateLight
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Run preset",
                                tint = TabloraSky,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Jobs
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Cleaning Sessions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TabloraNavy
                    )
                    Text(
                        text = "History",
                        fontSize = 12.sp,
                        color = TabloraSky,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigate(AppScreen.HISTORY) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(3).forEach { job ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(job.filename, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TabloraNavy)
                                    Text(
                                        "${job.originalRows} → ${job.finalRows} rows • ${job.operationsCount} operations",
                                        fontSize = 11.sp,
                                        color = TabloraSlateLight
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TabloraEmeraldLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(job.exportFormat, color = TabloraEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TabloraSky,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TabloraNavy)
            Text(title, fontSize = 10.sp, color = TabloraSlateLight, maxLines = 1)
        }
    }
}

@Composable
private fun SampleDatasetButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TabloraSkyLight)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TabloraNavy, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = TabloraSky, maxLines = 1)
        }
    }
}
