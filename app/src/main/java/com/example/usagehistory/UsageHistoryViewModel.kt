package com.example.usagehistory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.usagehistory.data.NotificationAccessManager
import com.example.usagehistory.data.UsageAccessManager
import com.example.usagehistory.data.UsageHistoryRepository
import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val sessions: List<UsageSessionEntity> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class UsageHistoryViewModel(
    private val repository: UsageHistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TimelineUiState())
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    fun onResume() {
        val hasAccess = repository.hasUsageAccess()
        val hasNotificationAccess = repository.hasNotificationAccess()
        _state.update { current ->
            current.copy(
                hasUsageAccess = hasAccess,
                hasNotificationAccess = hasNotificationAccess,
            )
        }
        if (hasAccess) {
            refreshCurrentDay()
        }
    }

    fun refreshCurrentDay() {
        loadDay(_state.value.selectedDate)
    }

    fun showPreviousDay() {
        loadDay(_state.value.selectedDate.minusDays(1))
    }

    fun showNextDay() {
        val candidate = _state.value.selectedDate.plusDays(1)
        val targetDate = minOf(candidate, LocalDate.now())
        loadDay(targetDate)
    }

    fun openUsageAccessSettings(context: Context) {
        UsageAccessManager.openUsageAccessSettings(context)
    }

    fun openNotificationAccessSettings(context: Context) {
        NotificationAccessManager.openNotificationAccessSettings(context)
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    selectedDate = date,
                    isRefreshing = true,
                    errorMessage = null,
                )
            }

            if (!repository.hasUsageAccess()) {
                _state.update { current ->
                    current.copy(
                        hasUsageAccess = false,
                        hasNotificationAccess = repository.hasNotificationAccess(),
                        isRefreshing = false,
                        sessions = emptyList(),
                    )
                }
                return@launch
            }

            runCatching {
                repository.refreshDay(date)
            }.onSuccess { sessions ->
                _state.update { current ->
                    current.copy(
                        selectedDate = date,
                        sessions = sessions,
                        hasUsageAccess = true,
                        hasNotificationAccess = repository.hasNotificationAccess(),
                        isRefreshing = false,
                    )
                }
            }.onFailure { throwable ->
                val cached = repository.loadCachedDay(date)
                _state.update { current ->
                    current.copy(
                        selectedDate = date,
                        sessions = cached,
                        hasUsageAccess = true,
                        hasNotificationAccess = repository.hasNotificationAccess(),
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "Failed to read usage history.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: UsageHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UsageHistoryViewModel(repository) as T
                }
            }
    }
}
