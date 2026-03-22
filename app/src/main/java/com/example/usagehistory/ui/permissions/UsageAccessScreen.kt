package com.example.usagehistory.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.usagehistory.ui.theme.PermissionBackground

@Composable
fun UsageAccessScreen(
    isChecking: Boolean,
    onGrantAccessClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PermissionBackground),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Usage history",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Grant Android Usage Access so the app can read foreground app sessions and draw your timeline.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "After enabling access in system settings, return here and refresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGrantAccessClick,
                ) {
                    Text("Grant access")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRefreshClick,
                ) {
                    Text("I already enabled it")
                }

                if (isChecking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
}
