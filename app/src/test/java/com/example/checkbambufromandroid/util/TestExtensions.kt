package com.example.checkbambufromandroid.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals

/**
 * Extension function to collect values from a Flow in a test environment
 * and assert them against an expected list of values.
 */
suspend fun <T> TestScope.assertFlowEmits(
    flow: Flow<T>,
    expectedValues: List<T>,
    message: String? = null
) {
    val actualValues = mutableListOf<T>()
    val job = launch {
        flow.toList(actualValues)
    }
    // Ensure the collection has a chance to run if flow emits slowly or is complex.
    // This might need adjustment based on flow complexity. For simple flows, it might not be needed.
    // advanceUntilIdle() // Alternative, if using TestCoroutineDispatcher directly

    // Basic check, more robust checks might be needed depending on the flow's nature
    // For instance, if the flow completes, job.join() or specific timing controls.
    // If the flow is a StateFlow or SharedFlow, initial values or replay cache might affect `toList`.

    assertEquals(message, expectedValues, actualValues)
    job.cancel() // Cancel the collection job once done.
}

/**
 * A simpler version for when you only care about the last emitted value of a flow
 * that is expected to emit multiple values but you're testing the final state.
 * Or for flows that emit a single value representing a result.
 */
suspend fun <T> TestScope.assertFlowLastEmit(
    flow: Flow<T>,
    expectedValue: T,
    action: suspend () -> Unit = {} // Action to trigger flow emission if needed
) {
    var lastValue: T? = null
    val job = launch {
        flow.collect {
            lastValue = it
        }
    }
    action() // Perform action that triggers flow
    // advanceUntilIdle() // Ensure coroutines complete

    assertEquals(expectedValue, lastValue)
    job.cancel()
}
