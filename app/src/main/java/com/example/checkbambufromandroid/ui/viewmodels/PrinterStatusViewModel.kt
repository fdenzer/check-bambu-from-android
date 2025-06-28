package com.example.checkbambufromandroid.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkbambufromandroid.data.BambuRepository
import com.example.checkbambufromandroid.data.Resource
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrinterStatusScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val printerStatus: PrinterStatusResponse? = null,
    val navigateToLogin: Boolean = false // To handle unauthorized/logout
)

@HiltViewModel
class PrinterStatusViewModel @Inject constructor(
    private val repository: BambuRepository
) : ViewModel() {

    private val _statusState = MutableStateFlow(PrinterStatusScreenState())
    val statusState: StateFlow<PrinterStatusScreenState> = _statusState.asStateFlow()

    init {
        fetchPrinterStatus()
    }

    fun fetchPrinterStatus() {
        viewModelScope.launch {
            repository.getPrinterStatus()
                .onEach { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _statusState.value = _statusState.value.copy(isLoading = true, error = null)
                        }
                        is Resource.Success -> {
                            _statusState.value = PrinterStatusScreenState(printerStatus = resource.data)
                        }
                        is Resource.Error -> {
                            val navigateToLogin = resource.message?.contains("Unauthorized", ignoreCase = true) == true ||
                                                  resource.message?.contains("401", ignoreCase = true) == true
                            _statusState.value = PrinterStatusScreenState(
                                error = resource.message,
                                navigateToLogin = navigateToLogin,
                                printerStatus = null // Clear old status on error
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun userLogout() {
        // Potentially clear session cookies here if repository/network layer doesn't handle it automatically on 401.
        // For now, just triggering navigation. The cookie jar in NetworkModule is in-memory,
        // so app restart or new login would effectively clear it.
        // A more robust solution for cookie clearing might be needed if cookies were persisted.
        _statusState.value = PrinterStatusScreenState(navigateToLogin = true)
    }

    fun resetNavigation() {
        _statusState.value = _statusState.value.copy(navigateToLogin = false, error = null)
    }

     fun clearError() {
        _statusState.value = _statusState.value.copy(error = null)
    }
}
