/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.resetPassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_reset_code_wrong
import mifos_mobile.feature.auth.generated.resources.feature_reset_enter_details
import mifos_mobile.feature.auth.generated.resources.feature_reset_submit
import mifos_mobile.feature.auth.generated.resources.feature_reset_title
import mifos_mobile.feature.auth.generated.resources.feature_set_new_password_confirm_password_label
import mifos_mobile.feature.auth.generated.resources.feature_set_new_password_new_password_label
import mifos_mobile.feature.auth.generated.resources.feature_signup_error_password_mismatch
import mifos_mobile.feature.auth.generated.resources.feature_verify_from_sms
import mifos_mobile.feature.auth.generated.resources.feature_verify_resend
import mifos_mobile.feature.auth.generated.resources.feature_verify_resend_in
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.mobile.core.designsystem.component.MifosButton
import org.mifos.mobile.core.designsystem.component.MifosPasswordField
import org.mifos.mobile.core.designsystem.component.MifosScaffold
import org.mifos.mobile.core.designsystem.theme.DesignToken
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.ui.component.MifosPoweredCard
import org.mifos.mobile.core.ui.component.MifosProgressIndicatorOverlay
import org.mifos.mobile.core.ui.utils.EventsEffect
import org.mifos.mobile.core.ui.utils.SmsCodeAutoRead
import org.mifos.mobile.feature.auth.components.CodeBoxes
import org.mifos.mobile.feature.auth.verifyCode.formatMinutes
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun ResetPasswordScreen(
    navigateToLogin: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            ResetPasswordEvent.NavigateToLogin -> navigateToLogin()
            ResetPasswordEvent.NavigateBack -> navigateBack()
        }
    }

    // The reset SMS carries the code too, so the same reader fills the boxes.
    SmsCodeAutoRead(requestKey = state.sendCount) { code ->
        viewModel.trySendAction(ResetPasswordAction.SmsCodeReceived(code))
    }

    ResetPasswordContent(
        state = state,
        modifier = modifier,
        onAction = remember(viewModel) { { viewModel.trySendAction(it) } },
    )
}

@Composable
private fun ResetPasswordContent(
    state: ResetPasswordState,
    onAction: (ResetPasswordAction) -> Unit,
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
                text = stringResource(Res.string.feature_reset_title),
                style = MifosTypography.headlineMedium,
                color = KptTheme.colorScheme.onBackground,
            )

            if (state.message.isNotBlank()) {
                Text(
                    text = state.message,
                    style = MifosTypography.bodyMedium,
                    color = KptTheme.colorScheme.secondary,
                )
            }

            Text(
                text = stringResource(Res.string.feature_reset_enter_details),
                style = MifosTypography.bodySmall,
                color = KptTheme.colorScheme.secondary,
            )

            CodeBoxes(
                code = state.code,
                onCodeChange = { onAction(ResetPasswordAction.CodeChanged(it)) },
                enabled = !state.isLoading,
                isError = state.error != null,
                length = state.codeLength,
            )

            if (state.codeFromSms) {
                Text(
                    text = stringResource(Res.string.feature_verify_from_sms),
                    style = MifosTypography.labelMedium,
                    color = KptTheme.colorScheme.primary,
                )
            }

            state.error?.let { error ->
                Text(text = errorText(error), style = MifosTypography.bodySmall, color = KptTheme.colorScheme.error)
            }

            MifosPasswordField(
                label = stringResource(Res.string.feature_set_new_password_new_password_label),
                value = state.password,
                onValueChange = { onAction(ResetPasswordAction.PasswordChanged(it)) },
                shape = KptTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                showPassword = state.showPassword,
                showPasswordChange = { onAction(ResetPasswordAction.TogglePassword) },
                isError = state.passwordError != null,
                hint = state.passwordError?.let { errorText(it) },
                keyboardType = KeyboardType.Password,
            )

            MifosPasswordField(
                label = stringResource(Res.string.feature_set_new_password_confirm_password_label),
                value = state.repeatPassword,
                onValueChange = { onAction(ResetPasswordAction.RepeatChanged(it)) },
                shape = KptTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                showPassword = state.showRepeat,
                showPasswordChange = { onAction(ResetPasswordAction.ToggleRepeat) },
                isError = state.repeatError != null,
                hint = state.repeatError?.let { errorText(it) },
                keyboardType = KeyboardType.Password,
            )

            MifosButton(
                modifier = Modifier.fillMaxWidth().height(DesignToken.sizes.buttonHeight),
                enabled = state.canSubmit,
                onClick = { onAction(ResetPasswordAction.Submit) },
                shape = KptTheme.shapes.medium,
            ) {
                Text(text = stringResource(Res.string.feature_reset_submit), style = MifosTypography.titleMedium)
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.resendSeconds > 0) {
                    Text(
                        text = stringResource(Res.string.feature_verify_resend_in, formatMinutes(state.resendSeconds)),
                        style = MifosTypography.labelMedium,
                        color = KptTheme.colorScheme.secondary,
                    )
                } else {
                    TextButton(onClick = { onAction(ResetPasswordAction.Resend) }, enabled = !state.isLoading) {
                        Text(text = stringResource(Res.string.feature_verify_resend))
                    }
                }
            }
        }

        if (state.isLoading) MifosProgressIndicatorOverlay()
    }
}

@Composable
private fun errorText(error: ResetError): String = when (error) {
    ResetError.CodeWrongOrExpired -> stringResource(Res.string.feature_reset_code_wrong)
    ResetError.Mismatch -> stringResource(Res.string.feature_signup_error_password_mismatch)
    is ResetError.Message -> error.text
}
