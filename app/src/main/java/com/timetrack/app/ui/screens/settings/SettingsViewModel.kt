package com.timetrack.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.domain.model.Category
import com.timetrack.app.domain.usecase.ExportCsv
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timerStateStore: TimerStateStore,
    private val exportCsv: ExportCsv,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val widgetCategoryId: StateFlow<Long?> = timerStateStore.widgetCategoryId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val sortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            categoryRepository.insert(
                Category(
                    name = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    isCustom = true,
                    sortOrder = sortOrder,
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val current = categories.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val reordered = current.apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch {
            val updated = reordered.mapIndexed { idx, cat -> cat.copy(sortOrder = idx) }
            categoryRepository.updateAll(updated)
        }
    }

    fun setWidgetCategory(categoryId: Long?) {
        viewModelScope.launch { timerStateStore.setWidgetCategory(categoryId) }
    }

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch { exportCsv(uri) }
    }
}
