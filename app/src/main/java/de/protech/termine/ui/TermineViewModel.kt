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

/** Ein Eintrag der Übersichtsliste: Überschrift oder Termin. */
sealed interface AgendaItem {
    data class Header(val label: String, val count: Int) : AgendaItem
    data class Entry(val full: AppointmentFull) : AgendaItem
}

data class HomeUiState(
    val next: AppointmentFull? = null,
    val agenda: List<AgendaItem> = emptyList(),
    val past: List<AppointmentFull> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activeFilter: Long? = null,
    val totalUpcoming: Int = 0,
    val loaded: Boolean = false
)

class TermineViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: TermineRepository = (app as TermineApp).repository

    private val filter = MutableStateFlow<Long?>(null)
    val activeFilter: StateFlow<Long?> = filter.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(repository.appointments, repository.categories, filter) { all, categories, active ->
            val now = System.currentTimeMillis()
            val visible = all.filter { active == null || it.appointment.categoryId == active }

            val upcoming = visible
                .filter { it.appointment.startMillis >= now }
                .sortedBy { it.appointment.startMillis }

            val past = visible
                .filter { it.appointment.startMillis < now }
                .sortedByDescending { it.appointment.startMillis }

            // Alles ausser dem obersten Termin wird in Zeitgruppen einsortiert.
            val rest = upcoming.drop(1)
            val agenda = buildList {
                var currentLabel: String? = null
                val grouped = rest.groupBy { groupLabel(it.appointment.startMillis, now) }
                rest.forEach { entry ->
                    val label = groupLabel(entry.appointment.startMillis, now)
                    if (label != currentLabel) {
                        add(AgendaItem.Header(label, grouped[label]?.size ?: 0))
                        currentLabel = label
                    }
                    add(AgendaItem.Entry(entry))
                }
            }

            HomeUiState(
                next = upcoming.firstOrNull(),
                agenda = agenda,
                past = past,
                categories = categories,
                activeFilter = active,
                totalUpcoming = upcoming.size,
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

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
