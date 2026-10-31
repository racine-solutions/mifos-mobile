/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.datastore.model

import kotlinx.serialization.Serializable
import org.mifos.mobile.core.model.LanguageConfig
import org.mifos.mobile.core.model.MifosThemeConfig

@Serializable
data class AppSettings(
    val userId: String,
    val tenant: String,
    val tenantName: String = "",
    val sessionEndedReason: String = "",
    /** The bank restricted this user to read-only access (banner plus no changes). */
    val isViewOnly: Boolean = false,
    val viewOnlyReason: String = "",
    /** Support numbers from the last login, for the view-only banner. */
    val supportPhoneNumbers: List<String> = emptyList(),
    /** Idle timeout sent by the server; [DEFAULT_SESSION_TIMEOUT_MINUTES] until the first value arrives. */
    val sessionTimeoutMinutes: Int = DEFAULT_SESSION_TIMEOUT_MINUTES,
    val baseUrl: String,
    val passcode: String,
    val appTheme: MifosThemeConfig,
    val sentTokenToServer: Boolean = false,
    val gcmToken: String? = null,
    val useDynamicColor: Boolean,
    val isAuthenticated: Boolean,
    val isUnlocked: Boolean,
    val language: LanguageConfig,
    val showOnboarding: Boolean,
    val firstTimeState: Boolean,
    val timeBasedTheme: TimeBasedTheme,
    val selectedServices: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_SESSION_TIMEOUT_MINUTES = 15

        val DEFAULT = AppSettings(
            userId = "",
            tenant = "",
//            baseUrl = "https://lms-backenddev.racinesolutions.com/",
            baseUrl = "https://localhost:8443/",
            appTheme = MifosThemeConfig.FOLLOW_SYSTEM,
            sentTokenToServer = false,
            gcmToken = null,
            language = LanguageConfig.DEFAULT,
            showOnboarding = true,
            firstTimeState = true,
            useDynamicColor = false,
            isAuthenticated = false,
            passcode = "",
            isUnlocked = false,
            timeBasedTheme = TimeBasedTheme(hourStart = 6, hourEnd = 0, timeStart = 17, timeEnd = 59),
        )
    }
}
