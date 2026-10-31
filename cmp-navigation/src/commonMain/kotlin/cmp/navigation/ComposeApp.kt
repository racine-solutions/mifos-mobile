/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package cmp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmp.navigation.rootnav.RootNavScreen
import cmp.navigation.utils.rememberIdleTimeoutModifier
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.mobile.core.designsystem.theme.MifosMobileTheme
import org.mifos.mobile.core.model.MifosThemeConfig
import org.mifos.mobile.core.ui.utils.EventsEffect
import org.mifos.mobile.core.ui.component.ViewOnlyBanner
import org.mifos.mobile.core.ui.utils.LocalViewOnly
import org.mifos.mobile.core.ui.utils.NetworkBanner

@Composable
fun ComposeApp(
    handleThemeMode: (osValue: Int) -> Unit,
    handleAppLocale: (locale: String?) -> Unit,
    onSplashScreenRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposeAppViewModel = koinViewModel(),
) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(eventFlow = viewModel.eventFlow) { event ->
        when (event) {
            is AppEvent.ShowToast -> {}
            is AppEvent.UpdateAppLocale -> handleAppLocale(event.localeName)
            is AppEvent.UpdateAppTheme -> handleThemeMode(event.osValue)
        }
    }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val accessState by viewModel.accessState.collectAsStateWithLifecycle()
    val sessionTimeoutMinutes by viewModel.sessionTimeoutMinutes.collectAsStateWithLifecycle()
    val idleTimeoutModifier = rememberIdleTimeoutModifier(
        enabled = isSessionActive,
        timeoutMillis = sessionTimeoutMinutes * MILLIS_PER_MINUTE,
        onTimeout = viewModel::logoutIdleSession,
    )

    LaunchedEffect(isSystemInDarkTheme, uiState.themeConfig) {
        if (uiState.themeConfig == MifosThemeConfig.FOLLOW_SYSTEM) {
            viewModel.trySendAction(AppAction.Internal.SystemThemeUpdate(isSystemInDarkTheme))
        }
    }

    MifosMobileTheme(
        darkTheme = uiState.darkTheme,
        androidTheme = uiState.isAndroidTheme,
        shouldDisplayDynamicTheming = uiState.isDynamicColorsEnabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(idleTimeoutModifier)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                NetworkBanner(
                    bannerState = uiState.networkBanner,
                    modifier = Modifier.fillMaxWidth(),
                )

                val viewOnly = accessState.isViewOnly && isSessionActive
                if (viewOnly) {
                    ViewOnlyBanner(phoneNumbers = accessState.supportPhoneNumbers)
                }

                CompositionLocalProvider(LocalViewOnly provides viewOnly) {
                    RootNavScreen(
                        modifier = Modifier,
                        onSplashScreenRemoved = onSplashScreenRemoved,
                    )
                }
            }
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
