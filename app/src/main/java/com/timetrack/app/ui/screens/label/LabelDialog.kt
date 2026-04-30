package com.timetrack.app.ui.screens.label

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timetrack.app.R
import com.timetrack.app.domain.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val colorPalette = listOf(
    "#5C6BC0", "#26C6DA", "#FFA726", "#66BB6A", "#42A5F5",
    "#EF5350", "#AB47BC", "#FFCA28", "#26A69A", "#FF7043",
    "#8D6E63", "#78909C",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDialog(
    sessionId: Long,
    onDismiss: () -> Unit,
    viewModel: LabelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateCategory by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header: session info
            uiState.session?.let { session ->
                val fmt = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
                val durationMin = session.durationMs / 60_000
                Text(
                    text = fmt.format(Date(session.startTimeMs)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "$durationMin min",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.label_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            CategoryGrid(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onSelect = viewModel::selectCategory,
                onAddNew = { showCreateCategory = true },
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(R.string.notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { viewModel.save(onDismiss) },
                    enabled = uiState.selectedCategory != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_save))
                }
            }
        }
    }

    if (showCreateCategory) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategory = false },
            onCreate = { name, colorHex, iconName ->
                viewModel.addCustomCategory(name, colorHex, iconName)
                showCreateCategory = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onSelect: (Category) -> Unit,
    onAddNew: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryCard(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onSelect(category) },
            )
        }
        // Add custom category button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onAddNew),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.create_category),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val color = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .then(
                if (isSelected)
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp),
                maxLines = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, iconName: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                stringResource(R.string.create_category),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.pick_color),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorPalette.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) }
                    catch (_: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selectedColor == hex)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = hex },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name.trim(), selectedColor, "category")
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_save))
                }
            }
        }
    }
}
