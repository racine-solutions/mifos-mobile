/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.verifyCode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_verify_button
import mifos_mobile.feature.auth.generated.resources.feature_verify_expired
import mifos_mobile.feature.auth.generated.resources.feature_verify_from_sms
import mifos_mobile.feature.auth.generated.resources.feature_verify_new_code
import mifos_mobile.feature.auth.generated.resources.feature_verify_resend
import mifos_mobile.feature.auth.generated.resources.feature_verify_resend_in
import mifos_mobile.feature.auth.generated.resources.feature_verify_send_email
import mifos_mobile.feature.auth.generated.resources.feature_verify_sent_email
import mifos_mobile.feature.auth.generated.resources.feature_verify_sent_email_fallback
import mifos_mobile.feature.auth.generated.resources.feature_verify_sent_sms
import mifos_mobile.feature.auth.generated.resources.feature_verify_title_email
import mifos_mobile.feature.auth.generated.resources.feature_verify_title_sms
import mifos_mobile.feature.auth.generated.resources.feature_verify_try_sms
import mifos_mobile.feature.auth.generated.resources.feature_verify_valid_for
import mifos_mobile.feature.auth.generated.resources.feature_verify_wrong_code
import mifos_mobile.feature.auth.generated.resources.feature_verify_wrong_code_tries
import mifos_mobile.feature.auth.generated.resources.feature_verify_wrong_number
import org.jetbrains.compose.resources.stringResource
import org.mifos.mobile.core.designsystem.component.MifosButton
import org.mifos.mobile.core.designsystem.component.MifosScaffold
import org.mifos.mobile.core.designsystem.theme.DesignToken
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.model.access.CodeChannel
import org.mifos.mobile.core.ui.component.MifosPoweredCard
import org.mifos.mobile.core.ui.component.MifosProgressIndicatorOverlay
import org.mifos.mobile.core.ui.utils.EventsEffect
import org.mifos.mobile.core.ui.utils.RacineContact
import org.mifos.mobile.core.ui.utils.ShareUtils
import org.mifos.mobile.core.ui.utils.SmsCodeAutoRead
import org.mifos.mobile.feature.auth.components.CodeBoxes
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun VerifyCodeScreen(
    navigateToLogin: () -> Unit,
    navigateBackToSignup: (String?) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyCodeViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            VerifyCodeEvent.NavigateToLogin -> navigateToLogin()
            VerifyCodeEvent.NavigateBack -> navigateBack()
            is VerifyCodeEvent.BackToSignup -> navigateBackToSignup(event.message)
        }
    }

    // The code SMS is read by the system (no SMS permission); it only fills the boxes.
    if (!state.isEmail && state.codeActive) {
        SmsCodeAutoRead(requestKey = state.sendCount) { code ->
            viewModel.trySendAction(VerifyCodeAction.SmsCodeReceived(code))
        }
    }

    VerifyCodeContent(
        state = state,
        modifier = modifier,
        onAction = remember(viewModel) { { viewModel.trySendAction(it) } },
    )
}

@Composable
private fun VerifyCodeContent(
    state: VerifyCodeState,
    onAction: (VerifyCodeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MifosScaffold(
        modifier = modifier,
        bottomBar = {
            Surface {
                MifosPoweredCard(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(KptTheme.spacing.md)
                .padding(top = KptTheme.spacing.md)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            Text(
                text = stringResource(
                    if (state.isEmail) Res.string.feature_verify_title_email else Res.string.feature_verify_title_sms,
                ),
                style = MifosTypography.headlineMedium,
                color = KptTheme.colorScheme.onBackground,
            )

            Text(
                text = sentMessage(state),
                style = MifosTypography.bodyMedium,
                color = KptTheme.colorScheme.secondary,
            )

            CodeBoxes(
                code = state.code,
                onCodeChange = { onAction(VerifyCodeAction.CodeChanged(it)) },
                enabled = state.codeActive && !state.isLoading,
                isError = state.error != null,
                length = state.codeLength,
                onDone = { onAction(VerifyCodeAction.Verify) },
            )

            if (state.codeFromSms) {
                Text(
                    text = stringResource(Res.string.feature_verify_from_sms),
                    style = MifosTypography.labelMedium,
                    color = KptTheme.colorScheme.primary,
                )
            }

            CodeErrorText(state)

            if (state.isEmail && state.codeActive && state.expirySeconds > 0) {
                Text(
                    text = stringResource(Res.string.feature_verify_valid_for, formatMinutes(state.expirySeconds)),
                    style = MifosTypography.labelMedium,
                    color = KptTheme.colorScheme.secondary,
                )
            }

            MifosButton(
                modifier = Modifier.fillMaxWidth().height(DesignToken.sizes.buttonHeight),
                enabled = state.canVerify,
                onClick = { onAction(VerifyCodeAction.Verify) },
                shape = KptTheme.shapes.medium,
            ) {
                Text(text = stringResource(Res.string.feature_verify_button), style = MifosTypography.titleMedium)
            }

            ResendRow(state, onAction)

            if (!state.isEmail) {
                WrongNumberRow(state)
            }

            Spacer(modifier = Modifier.height(KptTheme.spacing.md))
        }

        if (state.isLoading) MifosProgressIndicatorOverlay()
    }
}

@Composable
private fun sentMessage(state: VerifyCodeState): String = when {
    state.isEmail && state.smsFailed -> stringResource(Res.string.feature_verify_sent_email_fallback, state.destination)
    state.isEmail -> stringResource(Res.string.feature_verify_sent_email, state.destination)
    else -> stringResource(Res.string.feature_verify_sent_sms, state.destination)
}

@Composable
private fun CodeErrorText(state: VerifyCodeState) {
    val text = when (val error = state.error) {
        null -> return
        is CodeError.Wrong -> error.attemptsLeft
            ?.let { stringResource(Res.string.feature_verify_wrong_code_tries, it) }
            ?: stringResource(Res.string.feature_verify_wrong_code)
        CodeError.Expired -> stringResource(Res.string.feature_verify_expired)
        is CodeError.Message -> error.text
    }
    Text(text = text, style = MifosTypography.bodySmall, color = KptTheme.colorScheme.error)
}

@Composable
private fun ResendRow(state: VerifyCodeState, onAction: (VerifyCodeAction) -> Unit) {
    val otherChannel = if (state.isEmail) CodeChannel.SMS else CodeChannel.EMAIL
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!state.codeActive) {
            TextButton(onClick = { onAction(VerifyCodeAction.Resend(state.channel)) }, enabled = !state.isLoading) {
                Text(text = stringResource(Res.string.feature_verify_new_code))
            }
        } else if (state.resendSeconds > 0) {
            Text(
                text = stringResource(Res.string.feature_verify_resend_in, formatMinutes(state.resendSeconds)),
                style = MifosTypography.labelMedium,
                color = KptTheme.colorScheme.secondary,
            )
        } else {
            TextButton(onClick = { onAction(VerifyCodeAction.Resend(state.channel)) }, enabled = !state.isLoading) {
                Text(text = stringResource(Res.string.feature_verify_resend))
            }
        }

        TextButton(onClick = { onAction(VerifyCodeAction.Resend(otherChannel)) }, enabled = !state.isLoading) {
            Text(
                text = stringResource(
                    if (state.isEmail) Res.string.feature_verify_try_sms else Res.string.feature_verify_send_email,
                ),
            )
        }
    }
}

@Composable
private fun WrongNumberRow(state: VerifyCodeState) {
    val number = state.supportPhoneNumbers.firstOrNull() ?: RacineContact.PHONE_PRIMARY_DISPLAY
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { ShareUtils.openUrl("tel:${number.filter { it.isDigit() || it == '+' }}") },
        text = stringResource(Res.string.feature_verify_wrong_number, number),
        style = MifosTypography.labelMedium,
        color = KptTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}

/** Formats a number of seconds as m:ss, for the countdowns. */
internal fun formatMinutes(totalSeconds: Int): String {
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private const val SECONDS_PER_MINUTE = 60
