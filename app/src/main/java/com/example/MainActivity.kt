package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.ExportFormat
import com.example.ui.components.AskTabloraBottomSheet
import com.example.ui.components.ExportCenterDialog
import com.example.ui.components.OperationPreviewDialog
import com.example.ui.components.TabloraHeader
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataQualityScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NewJobScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TabloraBorder
import com.example.ui.theme.TabloraNavy
import com.example.ui.theme.TabloraSky
import com.example.ui.theme.TabloraSlate
import com.example.ui.theme.TabloraSlateLight
import com.example.viewmodel.TabloraViewModel
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TabloraApp()
            }
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun TabloraApp(viewModel: TabloraViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportDialog by remember { mutableStateOf(false) }
    var sidebarExpanded by remember { mutableStateOf(true) }

    // Handle back button navigation
    BackHandler(enabled = uiState.currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    // Show toast / snackbar messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val navItems = listOf(
        NavItem(AppScreen.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
        NavItem(AppScreen.NEW_JOB, Icons.Default.UploadFile, "New Job"),
        NavItem(AppScreen.WORKSPACE, Icons.Default.TableChart, "Workspace"),
        NavItem(AppScreen.QUALITY_REPORT, Icons.Default.Assessment, "Quality"),
        NavItem(AppScreen.PRESETS, Icons.Default.Bookmarks, "Presets"),
        NavItem(AppScreen.HISTORY, Icons.Default.History, "History"),
        NavItem(AppScreen.PRICING, Icons.Default.CreditCard, "Pricing"),
        NavItem(AppScreen.SETTINGS, Icons.Default.Settings, "Settings")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TabloraHeader(
                    canUndo = uiState.undoStack.isNotEmpty(),
                    canRedo = uiState.redoStack.isNotEmpty(),
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onReset = { viewModel.resetToOriginal() },
                    onAskAi = { viewModel.openAskAi() },
                    onExport = { showExportDialog = true },
                    hasDataset = uiState.currentDataset != null
                )
            },
            bottomBar = {
                // On mobile portrait, show clean BottomNavigationBar for primary views
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .border(androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder))
                            .navigationBarsPadding()
                    ) {
                        navItems.take(5).forEach { item ->
                            val selected = uiState.currentScreen == item.screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.screen) },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TabloraSky,
                                    selectedTextColor = TabloraSky,
                                    unselectedIconColor = TabloraSlateLight,
                                    unselectedTextColor = TabloraSlateLight,
                                    indicatorColor = TabloraSky.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_${item.screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Collapsible Desktop Sidebar
                if (isWideScreen) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(if (sidebarExpanded) 200.dp else 68.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TabloraBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            // Sidebar Toggle button
                            IconButton(
                                onClick = { sidebarExpanded = !sidebarExpanded },
                                modifier = Modifier.align(if (sidebarExpanded) Alignment.End else Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    imageVector = if (sidebarExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                                    contentDescription = "Toggle Sidebar",
                                    tint = TabloraSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            navItems.forEach { item ->
                                val selected = uiState.currentScreen == item.screen
                                val bg = if (selected) TabloraSky.copy(alpha = 0.12f) else Color.Transparent
                                val fg = if (selected) TabloraSky else TabloraSlate

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { viewModel.navigateTo(item.screen) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(20.dp))
                                    if (sidebarExpanded) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = fg
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Active Screen Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (uiState.currentScreen) {
                        AppScreen.DASHBOARD -> DashboardScreen(
                            currentDataset = uiState.currentDataset,
                            history = uiState.history,
                            presets = uiState.presets,
                            onNavigate = { viewModel.navigateTo(it) },
                            onLoadSample = { viewModel.loadSampleData(it) },
                            onApplyPreset = { viewModel.applyPreset(it) }
                        )

                        AppScreen.NEW_JOB -> NewJobScreen(
                            currentDataset = uiState.currentDataset,
                            defaultDelimiter = uiState.defaultCsvDelimiter,
                            isLoading = uiState.isLoading,
                            onLoadFile = { uri -> viewModel.loadFileFromUri(uri, context) },
                            onLoadSample = { viewModel.loadSampleData(it) },
                            onChangeDelimiter = { viewModel.setDefaultCsvDelimiter(it) },
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.QUALITY_REPORT -> DataQualityScreen(
                            currentDataset = uiState.currentDataset,
                            report = uiState.qualityReport,
                            onApplyOperation = { viewModel.applyOperation(it) },
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.WORKSPACE -> WorkspaceScreen(
                            dataset = uiState.currentDataset,
                            appliedOperations = uiState.appliedOperations,
                            canUndo = uiState.undoStack.isNotEmpty(),
                            canRedo = uiState.redoStack.isNotEmpty(),
                            density = uiState.density,
                            searchQuery = uiState.searchQuery,
                            sortColumn = uiState.sortColumn,
                            sortAscending = uiState.sortAscending,
                            hiddenColumns = uiState.hiddenColumns,
                            currentPage = uiState.currentPage,
                            pageSize = uiState.pageSize,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onSortChange = { viewModel.setSortColumn(it) },
                            onToggleColumnVisibility = { viewModel.toggleColumnVisibility(it) },
                            onPageChange = { viewModel.setPage(it) },
                            onPreviewOperation = { viewModel.previewOperation(it) },
                            onApplyOperation = { viewModel.applyOperation(it) },
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onReset = { viewModel.resetToOriginal() },
                            onOpenAskAi = { viewModel.openAskAi() },
                            onOpenExport = { showExportDialog = true },
                            onSaveAsPreset = { name, desc -> viewModel.saveCurrentOperationsAsPreset(name, desc) },
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.PRESETS -> PresetsScreen(
                            presets = uiState.presets,
                            currentDataset = uiState.currentDataset,
                            onApplyPreset = { viewModel.applyPreset(it) },
                            onDeletePreset = { viewModel.deletePreset(it) },
                            onResetPresets = { viewModel.resetPresets() },
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.HISTORY -> HistoryScreen(
                            history = uiState.history,
                            onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                            onClearAllHistory = { viewModel.clearHistory() },
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.PRICING -> PricingScreen(
                            onNavigate = { viewModel.navigateTo(it) }
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            currentDensity = uiState.density,
                            defaultExportFormat = uiState.defaultExportFormat,
                            defaultCsvDelimiter = uiState.defaultCsvDelimiter,
                            onDensityChange = { viewModel.setDensity(it) },
                            onExportFormatChange = { viewModel.setDefaultExportFormat(it) },
                            onCsvDelimiterChange = { viewModel.setDefaultCsvDelimiter(it) },
                            onResetPresets = { viewModel.resetPresets() },
                            onClearHistory = { viewModel.clearHistory() },
                            onClearAllData = { viewModel.clearAllData() },
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                }
            }
        }

        // Operation Preview Diff Dialog
        uiState.activeDiffPreview?.let { diff ->
            OperationPreviewDialog(
                diff = diff,
                onConfirm = { viewModel.applyOperation(diff.operation) },
                onDismiss = { viewModel.dismissDiffPreview() }
            )
        }

        // Ask Tablora AI Assistant Bottom Sheet
        if (uiState.showAskAiSheet) {
            AskTabloraBottomSheet(
                onDismiss = { viewModel.closeAskAi() },
                onSubmitPrompt = { prompt -> viewModel.submitAiPrompt(prompt) },
                onApplyPlan = { plan -> viewModel.applyAiPlan(plan) },
                proposedPlan = uiState.aiProposedPlan,
                isProcessing = uiState.aiIsProcessing
            )
        }

        // Export Center Dialog
        if (showExportDialog && uiState.currentDataset != null) {
            ExportCenterDialog(
                currentFilename = uiState.currentDataset!!.name,
                allColumns = uiState.currentDataset!!.columns,
                defaultFormat = uiState.defaultExportFormat,
                onDismiss = { showExportDialog = false },
                onExport = { format, filename, columns ->
                    val file = viewModel.exportDataset(context, format, filename, columns)
                    showExportDialog = false
                    if (file != null) {
                        shareExportedFile(context, file, format)
                    }
                }
            )
        }
    }
}

private fun shareExportedFile(context: android.content.Context, file: File, format: ExportFormat) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cleaned Spreadsheet from Tablora")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Spreadsheet"))
    } catch (_: Exception) {
        // Handled gracefully
    }
}
