package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
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
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraEmerald
import com.example.ui.theme.TabloraEmeraldLight
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSkyLight
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight

@Composable
fun PricingScreen(
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TabloraSkyLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("TRANSPARENT PRICING", color = TabloraSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Simple, predictable plans",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TabloraNavy)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tablora is 100% free and fully functional offline today. Future cloud plans are shown below for transparency.",
                    fontSize = 12.sp,
                    color = TabloraSlateLight,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Transparency Disclaimer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TabloraSky, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Notice: All cleaning, transformation, presets, and exports are completely free on this device. We do NOT collect credit cards, fake subscriptions, or mock checkouts.",
                        fontSize = 11.sp,
                        color = TabloraSlate
                    )
                }
            }
        }

        // Plan 1: Free Core
        item {
            PricingCard(
                tierName = "Free Core",
                price = "$0",
                cadence = "forever",
                badge = "CURRENT ACTIVE PLAN",
                badgeColor = TabloraEmerald,
                badgeBg = TabloraEmeraldLight,
                description = "Full-featured on-device spreadsheet cleaning for freelancers and researchers.",
                buttonText = "You are on Free Core",
                buttonEnabled = false,
                features = listOf(
                    "Full CSV and Excel (XLSX) import/export" to true,
                    "All 22 core cleaning and transformation operations" to true,
                    "Unlimited undo, redo, and reset" to true,
                    "Deterministic data quality score analysis" to true,
                    "Save & execute local cleaning presets" to true,
                    "Audit job history stored locally" to true,
                    "100% private, zero data upload" to true
                )
            )
        }

        // Plan 2: Pro Tier (Proposed)
        item {
            PricingCard(
                tierName = "Pro",
                price = "$9",
                cadence = "per month (proposed)",
                badge = "PLANNED CLOUD TIER",
                badgeColor = TabloraSky,
                badgeBg = TabloraSkyLight,
                description = "For ecommerce sellers and sales teams needing batch automation and cloud sync.",
                buttonText = "Planned Feature",
                buttonEnabled = false,
                features = listOf(
                    "Everything in Free Core" to true,
                    "Batch multi-file cleaning operations" to false,
                    "Automated cloud scheduled workflows" to false,
                    "Extended 100MB+ dataset memory optimization" to false,
                    "Advanced regex mapping libraries" to false
                )
            )
        }

        // Plan 3: Business Tier (Proposed)
        item {
            PricingCard(
                tierName = "Business",
                price = "$19",
                cadence = "per month (proposed)",
                badge = "PLANNED CLOUD TIER",
                badgeColor = Color(0xFF7C3AED),
                badgeBg = Color(0xFFEDE9FE),
                description = "For data teams, agencies, and operations managing high-volume client datasets.",
                buttonText = "Planned Feature",
                buttonEnabled = false,
                features = listOf(
                    "Everything in Pro" to true,
                    "Team preset sharing & collaborative rulebooks" to false,
                    "Custom ERP and CRM connectors" to false,
                    "Priority server-side processing" to false,
                    "Dedicated support and custom schemas" to false
                )
            )
        }
    }
}

@Composable
private fun PricingCard(
    tierName: String,
    price: String,
    cadence: String,
    badge: String,
    badgeColor: Color,
    badgeBg: Color,
    description: String,
    buttonText: String,
    buttonEnabled: Boolean,
    features: List<Pair<String, Boolean>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tierName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TabloraNavy)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TabloraNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text(cadence, fontSize = 12.sp, color = TabloraSlateLight, modifier = Modifier.padding(bottom = 4.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(description, fontSize = 12.sp, color = TabloraSlate)

            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { (feat, isLive) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TabloraEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feat, fontSize = 12.sp, color = TabloraNavy)
                        } else {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TabloraSlateLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$feat (Planned)", fontSize = 12.sp, color = TabloraSlateLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {},
                enabled = buttonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabloraSky,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = TabloraSlateLight
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
