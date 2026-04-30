package com.timetrack.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timetrack.app.BuildConfig
import com.timetrack.app.R
import com.timetrack.app.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val widgetCategoryId by viewModel.widgetCategoryId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? -> uri?.let { viewModel.exportToCsv(it) } }

    val powerManager = context.getSystemService(PowerManager::class.java)
    var isBatteryWhitelisted by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryWhitelisted =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var categoriesExpanded by remember { mutableStateOf(true) }
    var widgetSectionExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Reliability section
            SectionHeader(stringResource(R.string.settings_reliability))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_battery_optimization)) },
                supportingContent = {
                    Text(
                        if (isBatteryWhitelisted)
                            stringResource(R.string.settings_battery_whitelisted)
                        else
                            stringResource(R.string.settings_battery_not_whitelisted),
                        color = if (isBatteryWhitelisted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = { Icon(Icons.Default.BatteryFull, contentDescription = null) },
                trailingContent = {
                    if (!isBatteryWhitelisted) {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }) { Text(stringResource(R.string.settings_fix)) }
                    }
                },
            )

            // Widget section
            CollapsibleSectionHeader(
                title = stringResource(R.string.settings_widget),
                expanded = widgetSectionExpanded,
                onToggle = { widgetSectionExpanded = !widgetSectionExpanded },
                icon = { Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            )
            AnimatedVisibility(
                visible = widgetSectionExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_widget_category_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    // "None" option
                    WidgetCategoryRow(
                        name = stringResource(R.string.settings_widget_no_category),
                        colorHex = null,
                        selected = widgetCategoryId == null,
                        onClick = { viewModel.setWidgetCategory(null) },
                    )
                    categories.forEach { cat ->
                        WidgetCategoryRow(
                            name = cat.name,
                            colorHex = cat.colorHex,
                            selected = widgetCategoryId == cat.id,
                            onClick = { viewModel.setWidgetCategory(cat.id) },
                        )
                    }
                }
            }

            // Categories section
            CollapsibleSectionHeader(
                title = stringResource(R.string.settings_categories),
                expanded = categoriesExpanded,
                onToggle = { categoriesExpanded = !categoriesExpanded },
            )
            AnimatedVisibility(
                visible = categoriesExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_categories_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    CategoryDragList(
                        categories = categories,
                        onMove = viewModel::moveCategory,
                        onDelete = { categoryToDelete = it },
                    )
                    // Add category row
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_add_category)) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddDialog = true },
                    )
                }
            }

            // Data section
            SectionHeader(stringResource(R.string.settings_data))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export_csv)) },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { csvLauncher.launch("timetrack_export.csv") },
            )

            // About section
            SectionHeader(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // Delete confirmation dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_confirm, cat.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(cat)
                    categoryToDelete = null
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Add category dialog
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, colorHex ->
                viewModel.addCategory(name, colorHex, "")
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) icon()
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun WidgetCategoryRow(
    name: String,
    colorHex: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dotColor = colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) }
        catch (_: Exception) { null }
    }

    ListItem(
        headlineContent = { Text(name) },
        leadingContent = {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            } else {
                Box(modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                else Modifier
            ),
    )
}

@Composable
private fun CategoryDragList(
    categories: List<Category>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var accumulated by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    // Approximate item height (72dp is a standard ListItem height)
    val itemHeightPx = remember(density) { with(density) { 72.dp.toPx() } }

    Column {
        categories.forEachIndexed { index, category ->
            val isDragging = index == draggingIndex
            val bgColor by animateColorAsState(
                targetValue = if (isDragging)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    Color.Transparent,
                animationSpec = tween(150),
                label = "drag_bg",
            )

            Surface(
                color = bgColor,
                shadowElevation = if (isDragging) 4.dp else 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            category.name,
                            fontWeight = if (category.isCustom) FontWeight.Normal else FontWeight.Medium,
                        )
                    },
                    leadingContent = {
                        val dotColor = try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onDelete(category) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            // Drag handle — long press to drag
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = index
                                                accumulated = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                accumulated += dragAmount.y
                                                val threshold = itemHeightPx / 2f
                                                when {
                                                    accumulated < -threshold && draggingIndex > 0 -> {
                                                        onMove(draggingIndex, draggingIndex - 1)
                                                        draggingIndex -= 1
                                                        accumulated += itemHeightPx
                                                    }
                                                    accumulated > threshold && draggingIndex < categories.size - 1 -> {
                                                        onMove(draggingIndex, draggingIndex + 1)
                                                        draggingIndex += 1
                                                        accumulated -= itemHeightPx
                                                    }
                                                }
                                            },
                                            onDragEnd = { draggingIndex = -1; accumulated = 0f },
                                            onDragCancel = { draggingIndex = -1; accumulated = 0f },
                                        )
                                    },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, colorHex: String) -> Unit,
) {
    val presetColors = listOf(
        "#7A9971", "#B5806A", "#D4A574", "#7A8FA6", "#9B8EA6",
        "#6B9E8A", "#A6826B", "#8A9E6B", "#9E6B8A", "#6B8A9E",
    )
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(presetColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_add_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.category_color_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { hex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (hex == selectedColor)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
