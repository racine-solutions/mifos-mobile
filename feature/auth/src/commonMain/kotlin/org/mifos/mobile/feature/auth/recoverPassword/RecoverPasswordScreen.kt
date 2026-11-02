/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.recoverPassword

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_recover_now_log_in
import mifos_mobile.feature.auth.generated.resources.feature_recover_now_message
import mifos_mobile.feature.auth.generated.resources.feature_recover_now_remember_your_password
import mifos_mobile.feature.auth.generated.resources.feature_recover_identifier_label
import mifos_mobile.feature.auth.generated.resources.feature_recover_now_title
import mifos_mobile.feature.auth.generated.resources.feature_recover_send_code
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.mobile.core.designsystem.component.BasicDialogState
import org.mifos.mobile.core.designsystem.component.MifosBasicDialog
import org.mifos.mobile.core.designsystem.component.MifosButton
import org.mifos.mobile.core.designsystem.component.MifosOutlinedTextField
import org.mifos.mobile.core.designsystem.component.MifosScaffold
import org.mifos.mobile.core.designsystem.component.MifosTextFieldConfig
import org.mifos.mobile.core.designsystem.theme.DesignToken
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.ui.component.MifosPoweredCard
import org.mifos.mobile.core.ui.component.MifosProgressIndicatorOverlay
import org.mifos.mobile.core.ui.utils.EventsEffect
import org.mifos.mobile.core.ui.utils.ScreenUiState
import template.core.base.designsystem.theme.KptTheme

@Composable
internal fun RecoverPasswordScreen(
    navigateToResetPasswordScreen: () -> Unit,
    navigateToLoginScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecoverPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel.eventFlow) { event ->
        when (event) {
            RecoverPasswordEvent.NavigateToLogin -> navigateToLoginScreen.invoke()

            RecoverPasswordEvent.NavigateToResetPassword -> navigateToResetPasswordScreen.invoke()
        }
    }

    RecoverPasswordScreen(
        state = state,
        modifier = modifier,
        onAction = remember(viewModel) {
            { viewModel.trySendAction(it) }
        },
    )

    RecoverPasswordDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(RecoverPasswordAction.OnDismissDialog) }
        },
    )
}

@Composable
internal fun RecoverPasswordScreen(
    state: RecoverPasswordState,
    modifier: Modifier = Modifier,
    onAction: (RecoverPasswordAction) -> Unit,
) {
    MifosScaffold(
        modifier = modifier,
        bottomBar = {
            Surface {
                MifosPoweredCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        },
    ) {
        when (state.uiState) {
            ScreenUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(KptTheme.spacing.md)
                        .padding(top = KptTheme.spacing.md)
                        .statusBarsPadding(),
                ) {
                    Text(
                        text = stringResource(Res.string.feature_recover_now_title),
                        style = MifosTypography.headlineMedium,
                        color = KptTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(DesignToken.spacing.medium))

                    Text(
                        text = stringResource(Res.string.feature_recover_now_message),
                        style = MifosTypography.bodySmall,
                        color = KptTheme.colorScheme.secondary,
                    )

                    Spacer(modifier = Modifier.height(DesignToken.spacing.dp24))

                    ForgotPasswordInputBox(
                        state = state,
                        onAction = onAction,
                    )
                }

                if (state.showOverlay) {
                    MifosProgressIndicatorOverlay()
                }
            }

            else -> { }
        }
    }
}

@Composable
internal fun ForgotPasswordInputBox(
    state: RecoverPasswordState,
    onAction: (RecoverPasswordAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        MifosOutlinedTextField(
            value = state.identifier,
            onValueChange = { onAction(RecoverPasswordAction.OnIdentifierChange(it)) },
            label = stringResource(Res.string.feature_recover_identifier_label),
            shape = KptTheme.shapes.medium,
            textStyle = MifosTypography.bodyLarge,
            config = MifosTextFieldConfig(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
            ),
        )

        MifosButton(
            modifier = Modifier.fillMaxWidth().height(DesignToken.sizes.buttonHeight),
            enabled = state.isSendEnabled,
            onClick = {
                onAction(RecoverPasswordAction.OnSendCode)
            },
            shape = KptTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(Res.string.feature_recover_send_code),
                style = MifosTypography.titleMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.feature_recover_now_remember_your_password),
                style = MifosTypography.labelMedium,
                color = KptTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.width(DesignToken.spacing.dp6))

            Text(
                modifier = Modifier.clickable { onAction.invoke(RecoverPasswordAction.OnLogin) },
                text = stringResource(Res.string.feature_recover_now_log_in),
                style = MifosTypography.labelMediumEmphasized,
                color = KptTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RecoverPasswordDialogs(
    dialogState: RecoverPasswordState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is RecoverPasswordState.DialogState.Error -> {
            MifosBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Preview
@Composable
private fun PreviewRecoverPassword() {
    RecoverPasswordScreen(
        onAction = {},
        state = RecoverPasswordState(),
    )
}
