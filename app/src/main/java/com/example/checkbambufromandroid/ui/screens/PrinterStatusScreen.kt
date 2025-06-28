package com.example.checkbambufromandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.checkbambufromandroid.R
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import com.example.checkbambufromandroid.ui.theme.CheckBambuFromAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterStatusScreen(
    isLoading: Boolean,
    error: String?,
    printerStatus: PrinterStatusResponse?,
    onRefreshClicked: () -> Unit,
    onLogout: () -> Unit,
    clearError: () -> Unit
) {
    // Clear error when new data is loaded successfully or when loading starts
    LaunchedEffect(printerStatus, isLoading) {
        if (printerStatus != null || isLoading) {
            clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.status_title)) },
                actions = {
                    IconButton(onClick = {
                        clearError()
                        onRefreshClicked()
                    }, enabled = !isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(id = R.string.status_refresh_action))
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading && printerStatus == null) { // Initial loading
                    CircularProgressIndicator()
                } else if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        clearError()
                        onRefreshClicked()
                    }) {
                        Text(stringResource(id = R.string.status_retry_button))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onLogout) {
                        Text(stringResource(id = R.string.status_logout_button))
                    }
                } else if (printerStatus != null) {
                    StatusDetailRow(stringResource(id = R.string.status_label_status), printerStatus.formattedStatus)
                    StatusDetailRow(stringResource(id = R.string.status_label_progress), printerStatus.progress)
                    StatusDetailRow(stringResource(id = R.string.status_label_est_finish), printerStatus.estimatedFinishTime)
                    // Add more fields as they are defined in PrinterStatusResponse

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = {
                        clearError()
                        onRefreshClicked()
                        }, enabled = !isLoading) {
                        if (isLoading) { // Loading for refresh
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(id = R.string.status_refresh_action))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onLogout) {
                        Text(stringResource(id = R.string.status_logout_button))
                    }
                } else { // Should ideally not be reached if VM initializes with loading
                    Text(stringResource(id = R.string.status_no_status_available), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        clearError()
                        onRefreshClicked()
                    }, enabled = !isLoading) {
                         if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                         else Text(stringResource(id = R.string.status_button_refresh))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusDetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(value ?: stringResource(id = R.string.status_na), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun PrinterStatusScreenPreview() {
    CheckBambuFromAndroidTheme {
        PrinterStatusScreen(
            isLoading = false, error = null,
            printerStatus = PrinterStatusResponse("Printing", "50%", "11:00 PM"),
            {}, {}, {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrinterStatusScreenLoadingPreview() {
    CheckBambuFromAndroidTheme {
        PrinterStatusScreen(true, null, null, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun PrinterStatusScreenErrorPreview() {
    CheckBambuFromAndroidTheme {
        PrinterStatusScreen(false, "Failed to connect. Token expired.", null, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun PrinterStatusScreenRefreshingPreview() {
    CheckBambuFromAndroidTheme {
        PrinterStatusScreen(
            isLoading = true, error = null,
            printerStatus = PrinterStatusResponse("Printing", "50%", "11:00 PM"),
            {}, {}, {}
        )
    }
}
