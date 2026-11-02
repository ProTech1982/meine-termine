package de.protech.termine.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.protech.termine.TermineApp
import de.protech.termine.data.Appointment
import de.protech.termine.data.AppointmentFull
import de.protech.termine.data.Category
import de.protech.termine.data.TermineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListUiState(
    val upcoming: List<AppointmentFull> = emptyList(),
    val past: List<AppointmentFull> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activeFilter: Long? = null
)

class TermineViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: TermineRepository = (app as TermineApp).repository

    private val filter = MutableStateFlow<Long?>(null)
    val activeFilter: StateFlow<Long?> = filter.asStateFlow()

    val uiState: StateFlow<ListUiState> =
        combine(
            repository.appointments,
            repository.categories,
            filter
        ) { appointments, categories, activeFilter ->
            val now = System.currentTimeMillis()
            val visible = appointments.filter {
                activeFilter == null || it.appointment.categoryId == activeFilter
            }
            ListUiState(
                upcoming = visible
                    .filter { it.appointment.startMillis >= now }
                    .sortedBy { it.appointment.startMillis },
                past = visible
                    .filter { it.appointment.startMillis < now }
                    .sortedByDescending { it.appointment.startMillis },
                categories = categories,
                activeFilter = activeFilter
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    val categories: StateFlow<List<Category>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(categoryId: Long?) {
        filter.value = categoryId
    }

    fun appointment(id: Long) = repository.appointment(id)

    fun save(appointment: Appointment, minutesBefore: List<Int>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.save(appointment, minutesBefore)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(id)
            onDone()
        }
    }

    fun addCategory(name: String, colorArgb: Int) {
        viewModelScope.launch { repository.addCategory(name, colorArgb) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            if (filter.value == id) filter.value = null
        }
    }
}
