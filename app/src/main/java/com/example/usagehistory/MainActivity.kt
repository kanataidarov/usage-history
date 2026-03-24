package com.example.usagehistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.usagehistory.data.PackageMetadataResolver
import com.example.usagehistory.data.UsageEventReader
import com.example.usagehistory.data.UsageHistoryRepository
import com.example.usagehistory.data.local.AppDatabase
import com.example.usagehistory.ui.permissions.UsageAccessScreen
import com.example.usagehistory.ui.theme.UsageHistoryTheme
import com.example.usagehistory.ui.timeline.TimelineScreen

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        UsageHistoryRepository(
            context = applicationContext,
            database = AppDatabase.getInstance(applicationContext),
            usageEventReader = UsageEventReader(applicationContext),
            packageMetadataResolver = PackageMetadataResolver(applicationContext),
        )
    }

    private val viewModel by viewModels<UsageHistoryViewModel> {
        UsageHistoryViewModel.factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activity = this

        setContent {
            UsageHistoryTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val onOpenSettings = remember(viewModel) {
                    { viewModel.openUsageAccessSettings(activity) }
                }
                val onOpenNotificationSettings = remember(viewModel) {
                    { viewModel.openNotificationAccessSettings(activity) }
                }

                LifecycleResumeEffect(Unit) {
                    viewModel.onResume()
                    onPauseOrDispose { }
                }

                if (!state.hasUsageAccess) {
                    UsageAccessScreen(
                        isChecking = state.isRefreshing,
                        onGrantAccessClick = onOpenSettings,
                        hasNotificationAccess = state.hasNotificationAccess,
                        onGrantNotificationAccessClick = onOpenNotificationSettings,
                        onRefreshClick = viewModel::refreshCurrentDay,
                    )
                } else {
                    TimelineScreen(
                        state = state,
                        onRefresh = viewModel::refreshCurrentDay,
                        onPreviousDay = viewModel::showPreviousDay,
                        onNextDay = viewModel::showNextDay,
                        onGrantNotificationAccessClick = onOpenNotificationSettings,
                    )
                }
            }
        }
    }
}
