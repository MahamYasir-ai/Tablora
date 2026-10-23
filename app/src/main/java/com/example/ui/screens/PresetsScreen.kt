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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.CleaningPreset
import com.example.model.SpreadsheetDataset
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraEmerald
import com.example.ui.theme.TabloraEmeraldLight
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraRose
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSkyLight
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

@Composable
fun PresetsScreen(
    presets: List<CleaningPreset>,
    currentDataset: SpreadsheetDataset?,
    onApplyPreset: (CleaningPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onResetPresets: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reusable Cleaning Workflows",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TabloraNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Apply automated multi-step cleaning routines to datasets in one tap. Stored locally in your browser storage.",
                        fontSize = 12.sp,
                        color = TabloraSlateLight
                    )
                }

                OutlinedButton(
                    onClick = onResetPresets,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Defaults", fontSize = 11.sp)
                }
            }
        }

        items(presets) { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preset_card_${preset.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TabloraNavy
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (preset.isBuiltIn) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TabloraSkyLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Built-in", fontSize = 10.sp, color = TabloraSky, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (!preset.isBuiltIn) {
                            IconButton(onClick = { onDeletePreset(preset.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = TabloraRose)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = preset.description,
                        fontSize = 12.sp,
                        color = TabloraSlate
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Transformation Sequence (${preset.operations.size} steps):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TabloraSlateLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        preset.operations.forEachIndexed { idx, op ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}. ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TabloraNavy)
                                Text(op.displayName, fontSize = 11.sp, color = TabloraSlate)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (currentDataset != null) {
                                onApplyPreset(preset)
                                onNavigate(AppScreen.WORKSPACE)
                            } else {
                                onNavigate(AppScreen.NEW_JOB)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TabloraSky),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (currentDataset != null) "Apply to Current Dataset" else "Load Dataset to Run",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
