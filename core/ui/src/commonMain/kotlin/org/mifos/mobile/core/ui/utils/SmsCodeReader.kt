/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reads a one-time code from an incoming SMS without asking for the SMS permission. Only Android
 * provides a real reader; everywhere else the member types the code.
 */
interface SmsCodeReader {
    /**
     * Starts listening for one code message and calls [onCode] with its 6 digits. Returns a function
     * that stops listening. Any failure or timeout ends silently, so manual entry always works.
     */
    fun start(onCode: (String) -> Unit): () -> Unit
}

object NoSmsCodeReader : SmsCodeReader {
    override fun start(onCode: (String) -> Unit): () -> Unit = {}
}

val LocalSmsCodeReader = staticCompositionLocalOf<SmsCodeReader> { NoSmsCodeReader }

/** Pulls the first 6-digit number out of an SMS body. */
fun extractSixDigitCode(message: String): String? = SIX_DIGITS.find(message)?.value

private val SIX_DIGITS = Regex("\\d{6}")

/**
 * Listens for the code SMS while this is on screen. [requestKey] changes every time a new code is
 * requested, which restarts the listener. The code is only handed over, never submitted.
 */
@Composable
fun SmsCodeAutoRead(requestKey: Int, onCode: (String) -> Unit) {
    val reader = LocalSmsCodeReader.current
    val latestOnCode by rememberUpdatedState(onCode)
    DisposableEffect(reader, requestKey) {
        val stop = reader.start { code -> latestOnCode(code) }
        onDispose { stop() }
    }
}
