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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.CleaningOperation
import com.example.model.DataQualityReport
import com.example.model.QualityIssue
import com.example.model.SpreadsheetDataset
import com.example.ui.components.SeverityChip
import com.example.ui.theme.TabloraAmber
import com.example.ui.theme.TabloraAmberLight
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraEmerald
import com.example.ui.theme.TabloraEmeraldLight
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraRose
import com.example.ui.theme.TabloraRoseLight
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSkyLight
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

@Composable
fun DataQualityScreen(
    currentDataset: SpreadsheetDataset?,
    report: DataQualityReport?,
    onApplyOperation: (CleaningOperation) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentDataset == null || report == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TabloraSlateLight, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No active dataset loaded", fontWeight = FontWeight.Bold, color = TabloraNavy)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onNavigate(AppScreen.NEW_JOB) },
                    colors = ButtonDefaults.buttonColors(containerColor = TabloraSky)
                ) {
                    Text("Upload or Select Sample")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quality Score Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dataset Quality Score",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TabloraNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentDataset.name} • ${currentDataset.rowCount} rows × ${currentDataset.columnCount} columns",
                                fontSize = 12.sp,
                                color = TabloraSlateLight
                            )
                        }

                        // Quality Score Badge
                        val scoreColor = when {
                            report.qualityScore >= 85 -> TabloraEmerald
                            report.qualityScore >= 60 -> TabloraAmber
                            else -> TabloraRose
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.15f))
                                .border(2.dp, scoreColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${report.qualityScore}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = scoreColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Documented Formula:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TabloraSlate
                            )
                            Text(
                                text = report.scoreFormula,
                                fontSize = 10.sp,
                                color = TabloraSlateLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetric(
                            label = "Empty Rows",
                            value = "${report.emptyRowsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetric(
                            label = "Duplicate Rows",
                            value = "${report.duplicateRowsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetric(
                            label = "Missing Cells",
                            value = "${report.emptyCellsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetric(
                            label = "Total Issues",
                            value = "${report.totalIssuesCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Quality Issues (${report.categories.size} categories)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TabloraNavy
                )
                Button(
                    onClick = { onNavigate(AppScreen.WORKSPACE) },
                    colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Workspace", fontSize = 12.sp)
                }
            }
        }

        // Issue Categories
        if (report.categories.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TabloraEmeraldLight)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TabloraEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Zero issues detected! This dataset is fully clean and ready for export.",
                            fontWeight = FontWeight.Bold,
                            color = TabloraNavy,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(report.categories) { category ->
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
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TabloraNavy
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TabloraSkyLight)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${category.totalCount} issue(s)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TabloraSky
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            category.issues.forEach { issue ->
                                IssueRow(
                                    issue = issue,
                                    onAutoFix = {
                                        if (issue.suggestedOperation != null) {
                                            onApplyOperation(issue.suggestedOperation)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TabloraNavy)
            Text(label, fontSize = 10.sp, color = TabloraSlateLight, maxLines = 1)
        }
    }
}

@Composable
private fun IssueRow(
    issue: QualityIssue,
    onAutoFix: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, TabloraBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(issue.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TabloraNavy)
            SeverityChip(severity = issue.severity)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(issue.description, fontSize = 12.sp, color = TabloraSlate)

        if (issue.affectedColumns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Affected: ${issue.affectedColumns.joinToString(", ")}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TabloraSlateLight
            )
        }

        if (issue.sampleValues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Samples: ${issue.sampleValues.joinToString(", ")}",
                fontSize = 10.sp,
                color = TabloraRose
            )
        }

        if (issue.suggestedOperation != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAutoFix,
                colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("autofix_${issue.id}")
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fix with ${issue.suggestedOperation.displayName}", fontSize = 11.sp)
            }
        }
    }
}
