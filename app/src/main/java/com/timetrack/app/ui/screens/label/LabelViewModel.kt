package com.timetrack.app.ui.screens.label

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Category
import com.timetrack.app.domain.model.Session
import com.timetrack.app.domain.usecase.LabelSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabelUiState(
    val session: Session? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val notes: String = "",
    val isSaved: Boolean = false,
)

@HiltViewModel
class LabelViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
    private val labelSession: LabelSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelUiState())
    val uiState: StateFlow<LabelUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabelUiState())

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getById(sessionId) ?: return@launch
            _uiState.value = _uiState.value.copy(
                session = session,
                selectedCategory = session.category,
                notes = session.notes ?: "",
            )
        }
        viewModelScope.launch {
            categoryRepository.getAll().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        val session = state.session ?: return
        val category = state.selectedCategory ?: return
        viewModelScope.launch {
            labelSession(session.id, category, state.notes.takeIf { it.isNotBlank() })
            _uiState.value = _uiState.value.copy(isSaved = true)
            onDone()
        }
    }

    fun addCustomCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val sortOrder = (_uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val id = categoryRepository.insert(
                Category(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName,
                    isCustom = true,
                    sortOrder = sortOrder,
                )
            )
            val created = categoryRepository.getById(id)
            if (created != null) selectCategory(created)
        }
    }
}
