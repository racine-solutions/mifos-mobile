/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package cmp.navigation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlin.time.Clock

/**
 * Tracks user interaction and invokes [onTimeout] once the user has been idle for
 * [timeoutMillis] while [enabled] is true.
 *
 * Time is measured with the wall clock, so time spent with the app in the background (or the
 * device asleep) counts as idle time. The elapsed time is re-checked whenever the app resumes.
 *
 * @return a [Modifier] that must be applied to the root of the UI so that every pointer and key
 * event resets the idle timer. Events are observed but never consumed.
 */
@Composable
fun rememberIdleTimeoutModifier(
    enabled: Boolean,
    timeoutMillis: Long,
    onTimeout: () -> Unit,
): Modifier {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    // The server can change the timeout while the app is running, so always read the latest value.
    val currentTimeoutMillis by rememberUpdatedState(timeoutMillis)
    val tracker = remember { IdleTracker() }

    // Start counting from "now" every time the session becomes active.
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        tracker.reset()
        while (true) {
            val remaining = currentTimeoutMillis - tracker.idleMillis()
            if (remaining <= 0) {
                currentOnTimeout()
                break
            }
            // Wake up regularly so a shorter timeout from the server takes effect promptly.
            delay(minOf(remaining, MAX_WAIT_MILLIS))
        }
    }

    // Timers can be paused while the device sleeps, so re-check when the app returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (enabled && event == Lifecycle.Event.ON_RESUME && tracker.idleMillis() >= currentTimeoutMillis) {
                currentOnTimeout()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return Modifier
        .onPreviewKeyEvent {
            tracker.reset()
            false
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                    tracker.reset()
                }
            }
        }
}

private class IdleTracker {
    private var lastInteractionMillis: Long = now()

    fun reset() {
        lastInteractionMillis = now()
    }

    fun idleMillis(): Long = now() - lastInteractionMillis

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}

private const val MAX_WAIT_MILLIS = 30_000L
