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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_reset_password_changed
import org.jetbrains.compose.resources.getString
import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.data.repository.SelfServiceAccessRepository
import org.mifos.mobile.core.datastore.UserPreferencesRepository
import org.mifos.mobile.core.model.ApiError
import org.mifos.mobile.core.model.ApiErrorCodes
import org.mifos.mobile.core.model.ApiException
import org.mifos.mobile.core.model.access.CODE_LENGTH
import org.mifos.mobile.core.model.access.PasswordResetStarted
import org.mifos.mobile.core.ui.utils.BaseViewModel
import org.mifos.mobile.feature.auth.components.ContactBlock
import org.mifos.mobile.feature.auth.components.asContactBlock
import org.mifos.mobile.feature.auth.session.AuthHandoff

/**
 * Second step of "forgot password": the 6-digit code plus the new password. A successful reset
 * also unlocks an account locked by wrong passwords, and ends every earlier session.
 */
internal class ResetPasswordViewModel(
    private val repository: SelfServiceAccessRepository,
    private val preferences: UserPreferencesRepository,
    private val authHandoff: AuthHandoff,
) : BaseViewModel<ResetPasswordState, ResetPasswordEvent, ResetPasswordAction>(
    initialState = ResetPasswordState(),
) {

    private var ticker: Job? = null

    init {
        val started = authHandoff.takeResetStarted()
        if (started == null) {
            // Nothing to continue (the app was restarted), so go back to ask for a new code.
            viewModelScope.launch { sendEvent(ResetPasswordEvent.NavigateBack) }
        } else {
            apply(started)
        }
    }

    override fun handleAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.CodeChanged -> update { it.copy(code = action.code, error = null, codeFromSms = false) }

            is ResetPasswordAction.SmsCodeReceived -> update { it.copy(code = action.code, error = null, codeFromSms = true) }

            is ResetPasswordAction.PasswordChanged -> update { it.copy(password = action.password, passwordError = null) }

            is ResetPasswordAction.RepeatChanged -> update { it.copy(repeatPassword = action.password, repeatError = null) }

            ResetPasswordAction.TogglePassword -> update { it.copy(showPassword = !it.showPassword) }

            ResetPasswordAction.ToggleRepeat -> update { it.copy(showRepeat = !it.showRepeat) }

            ResetPasswordAction.Submit -> submit()

            ResetPasswordAction.Resend -> resend()

            ResetPasswordAction.Back -> sendEvent(ResetPasswordEvent.NavigateBack)

            ResetPasswordAction.DismissBlocked -> update { it.copy(blocked = null) }
        }
    }

    private fun apply(started: PasswordResetStarted) {
        update {
            it.copy(
                resetId = started.resetId,
                message = started.message,
                codeLength = started.codeLength,
                expirySeconds = started.codeExpiresInSeconds,
                resendSeconds = started.resendAfterSeconds,
                code = "",
                codeFromSms = false,
                error = null,
                isLoading = false,
                sendCount = it.sendCount + 1,
            )
        }
        startTicker()
    }

    private fun submit() {
        val current = state
        if (!current.canSubmit) return
        if (current.password != current.repeatPassword) {
            viewModelScope.launch {
                update { it.copy(repeatError = ResetError.Mismatch) }
            }
            return
        }
        update { it.copy(isLoading = true, error = null, passwordError = null) }
        viewModelScope.launch {
            val result = repository.passwordResetVerify(
                resetId = current.resetId,
                code = current.code,
                password = current.password,
                repeatPassword = current.repeatPassword,
            )
            when (result) {
                is DataState.Success -> onReset(result.data.username)
                is DataState.Error -> onError(result)
                DataState.Loading -> Unit
            }
        }
    }

    private suspend fun onReset(username: String) {
        // Every earlier session was revoked by the server, so drop anything stored for one.
        if (preferences.settingsInfo.value.isAuthenticated) preferences.logOut()
        authHandoff.loginUsername = username
        authHandoff.loginNotice = getString(Res.string.feature_reset_password_changed)
        update { it.copy(isLoading = false, password = "", repeatPassword = "", code = "") }
        sendEvent(ResetPasswordEvent.NavigateToLogin)
    }

    private fun onError(result: DataState.Error<*>) {
        val error = apiError(result)
        when (error?.code) {
            ApiErrorCodes.CONTACT_CONFLICT -> update {
                it.copy(isLoading = false, code = "", blocked = error.asContactBlock())
            }

            // No attempts counter by design: the code is simply wrong or too old.
            ApiErrorCodes.CODE_INVALID -> update {
                it.copy(isLoading = false, code = "", error = ResetError.CodeWrongOrExpired)
            }

            ApiErrorCodes.INVALID_REQUEST -> update {
                it.copy(isLoading = false, passwordError = ResetError.Message(error.userMessage))
            }

            else -> update {
                it.copy(isLoading = false, error = ResetError.Message(error?.userMessage ?: result.message))
            }
        }
    }

    private fun resend() {
        val current = state
        if (current.isLoading || current.resendSeconds > 0) return
        update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.passwordResetResend(current.resetId)) {
                is DataState.Success -> apply(result.data.copy(resetId = result.data.resetId.ifBlank { current.resetId }))
                is DataState.Error -> update {
                    it.copy(isLoading = false, error = ResetError.Message(apiError(result)?.userMessage ?: result.message))
                }
                DataState.Loading -> Unit
            }
        }
    }

    private fun apiError(result: DataState.Error<*>): ApiError? = (result.exception as? ApiException)?.error

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (state.resendSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
                update { it.copy(resendSeconds = (it.resendSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun update(block: (ResetPasswordState) -> ResetPasswordState) = mutableStateFlow.update(block)

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
    }
}

internal data class ResetPasswordState(
    val resetId: String = "",
    /** The neutral sentence the server sent, shown as is. */
    val message: String = "",
    val code: String = "",
    val codeLength: Int = CODE_LENGTH,
    val codeFromSms: Boolean = false,
    val password: String = "",
    val repeatPassword: String = "",
    val showPassword: Boolean = false,
    val showRepeat: Boolean = false,
    val expirySeconds: Int = 0,
    val resendSeconds: Int = 0,
    val error: ResetError? = null,
    val passwordError: ResetError? = null,
    val repeatError: ResetError? = null,
    val isLoading: Boolean = false,
    val blocked: ContactBlock? = null,
    val sendCount: Int = 0,
) {
    val canSubmit: Boolean
        get() = code.length == codeLength && password.isNotEmpty() && repeatPassword.isNotEmpty() && !isLoading
}

internal sealed interface ResetError {
    data object CodeWrongOrExpired : ResetError
    data object Mismatch : ResetError
    data class Message(val text: String) : ResetError
}

internal sealed interface ResetPasswordEvent {
    data object NavigateToLogin : ResetPasswordEvent
    data object NavigateBack : ResetPasswordEvent
}

internal sealed interface ResetPasswordAction {
    data class CodeChanged(val code: String) : ResetPasswordAction
    data class SmsCodeReceived(val code: String) : ResetPasswordAction
    data class PasswordChanged(val password: String) : ResetPasswordAction
    data class RepeatChanged(val password: String) : ResetPasswordAction
    data object TogglePassword : ResetPasswordAction
    data object ToggleRepeat : ResetPasswordAction
    data object Submit : ResetPasswordAction
    data object Resend : ResetPasswordAction
    data object Back : ResetPasswordAction
    data object DismissBlocked : ResetPasswordAction
}
