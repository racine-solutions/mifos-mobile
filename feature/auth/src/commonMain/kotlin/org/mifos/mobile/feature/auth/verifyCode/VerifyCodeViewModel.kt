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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_verify_account_ready
import org.jetbrains.compose.resources.getString
import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.data.repository.SelfServiceAccessRepository
import org.mifos.mobile.core.datastore.UserPreferencesRepository
import org.mifos.mobile.core.model.ApiError
import org.mifos.mobile.core.model.ApiErrorCodes
import org.mifos.mobile.core.model.ApiException
import org.mifos.mobile.core.model.access.CODE_LENGTH
import org.mifos.mobile.core.model.access.CodeChannel
import org.mifos.mobile.core.model.access.CodeDelivery
import org.mifos.mobile.core.model.access.SignupResume
import org.mifos.mobile.core.ui.utils.BaseViewModel
import org.mifos.mobile.feature.auth.session.AuthHandoff

/**
 * Finishes a sign-up with the 6-digit code. It can start from the sign-up form (the answer is in
 * [AuthHandoff]) or resume a sign-up whose reference is stored on the device.
 */
internal class VerifyCodeViewModel(
    private val repository: SelfServiceAccessRepository,
    private val preferences: UserPreferencesRepository,
    private val authHandoff: AuthHandoff,
) : BaseViewModel<VerifyCodeState, VerifyCodeEvent, VerifyCodeAction>(
    initialState = VerifyCodeState(),
) {

    private var ticker: Job? = null

    init {
        viewModelScope.launch { loadSignup() }
    }

    override fun handleAction(action: VerifyCodeAction) {
        when (action) {
            is VerifyCodeAction.CodeChanged -> updateState {
                it.copy(code = action.code, error = null, codeFromSms = false)
            }

            is VerifyCodeAction.SmsCodeReceived -> updateState {
                if (it.codeActive) it.copy(code = action.code, error = null, codeFromSms = true) else it
            }

            VerifyCodeAction.Verify -> verify()

            is VerifyCodeAction.Resend -> resend(action.channel)

            VerifyCodeAction.Back -> sendEvent(VerifyCodeEvent.NavigateBack)
        }
    }

    private suspend fun loadSignup() {
        val delivery = authHandoff.takeSignupDelivery()
        val registrationId = delivery?.registrationId ?: preferences.settingsInfo.value.signupRegistrationId
        if (registrationId.isBlank()) {
            backToSignup(null)
            return
        }
        updateState { it.copy(registrationId = registrationId) }
        if (delivery != null) {
            applyDelivery(delivery)
        } else {
            resume(registrationId)
        }
    }

    private suspend fun resume(registrationId: String) {
        updateState { it.copy(isLoading = true) }
        when (val result = repository.signupResume(registrationId)) {
            is DataState.Success -> applyResume(result.data)
            is DataState.Error -> {
                val error = apiError(result)
                if (error?.code == ApiErrorCodes.SIGNUP_NOT_FOUND) {
                    forgetSignup()
                    backToSignup(error.userMessage)
                } else {
                    updateState {
                        it.copy(isLoading = false, error = CodeError.Message(error?.userMessage ?: result.message))
                    }
                }
            }
            DataState.Loading -> Unit
        }
    }

    private fun applyResume(resume: SignupResume) {
        updateState {
            it.copy(
                isLoading = false,
                channel = resume.channel,
                destination = resume.destination,
                codeLength = resume.codeLength,
                codeActive = resume.codeActive,
                expirySeconds = resume.codeExpiresInSeconds,
                resendSeconds = resume.resendAfterSeconds,
                attemptsLeft = resume.attemptsLeft,
                code = "",
                error = if (resume.codeActive) null else CodeError.Expired,
                // Resuming does not send a new SMS, so there is nothing for the reader to wait for.
                sendCount = it.sendCount,
            )
        }
        startTicker()
    }

    private fun applyDelivery(delivery: CodeDelivery) {
        updateState {
            it.copy(
                registrationId = delivery.registrationId,
                isLoading = false,
                channel = delivery.channel,
                destination = delivery.destination,
                smsFailed = delivery.smsFailed,
                codeLength = delivery.codeLength,
                codeActive = true,
                expirySeconds = delivery.codeExpiresInSeconds,
                resendSeconds = delivery.resendAfterSeconds,
                attemptsLeft = null,
                code = "",
                codeFromSms = false,
                error = null,
                sendCount = it.sendCount + 1,
            )
        }
        startTicker()
    }

    private fun verify() {
        val current = state
        if (current.isLoading || !current.codeActive || current.code.length != current.codeLength) return
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.signupVerify(current.registrationId, current.code)) {
                is DataState.Success -> onVerified(result.data.username)
                is DataState.Error -> onVerifyError(result)
                DataState.Loading -> Unit
            }
        }
    }

    private suspend fun onVerified(username: String) {
        forgetSignup()
        val password = authHandoff.signupPassword
        authHandoff.clearSignupSecrets()
        if (password != null) {
            // Log in right away with what the member typed; the password never touched the disk.
            authHandoff.autoLoginUsername = username
            authHandoff.autoLoginPassword = password
        } else {
            // Resumed after the app was closed, so the password is gone: ask for it at login.
            authHandoff.loginUsername = username
            authHandoff.loginNotice = getString(Res.string.feature_verify_account_ready)
        }
        updateState { it.copy(isLoading = false) }
        sendEvent(VerifyCodeEvent.NavigateToLogin)
    }

    private suspend fun onVerifyError(result: DataState.Error<*>) {
        val error = apiError(result)
        when (error?.code) {
            ApiErrorCodes.CODE_INVALID -> updateState {
                it.copy(isLoading = false, code = "", attemptsLeft = error.attemptsLeft, error = CodeError.Wrong(error.attemptsLeft))
            }

            ApiErrorCodes.CODE_EXPIRED -> updateState {
                it.copy(isLoading = false, code = "", codeActive = false, expirySeconds = 0, error = CodeError.Expired)
            }

            ApiErrorCodes.SIGNUP_NOT_FOUND -> {
                forgetSignup()
                backToSignup(error.userMessage)
            }

            else -> updateState {
                it.copy(
                    isLoading = false,
                    error = CodeError.Message(error?.userMessage ?: result.message),
                    supportPhoneNumbers = error?.supportPhoneNumbers.orEmpty().ifEmpty { it.supportPhoneNumbers },
                )
            }
        }
    }

    private fun resend(channel: String) {
        val current = state
        if (current.isLoading) return
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.signupResend(current.registrationId, channel)) {
                is DataState.Success -> applyDelivery(result.data)
                is DataState.Error -> onResendError(result)
                DataState.Loading -> Unit
            }
        }
    }

    private suspend fun onResendError(result: DataState.Error<*>) {
        val error = apiError(result)
        when (error?.code) {
            ApiErrorCodes.SIGNUP_NOT_FOUND -> {
                forgetSignup()
                backToSignup(error.userMessage)
            }

            ApiErrorCodes.CODE_RESEND_TOO_SOON, ApiErrorCodes.CODE_SEND_LIMIT -> {
                updateState {
                    it.copy(
                        isLoading = false,
                        resendSeconds = error.retryAfterSeconds ?: DEFAULT_RESEND_SECONDS,
                        error = CodeError.Message(error.userMessage),
                    )
                }
                startTicker()
            }

            else -> {
                // Delivery failures keep their reference so the member can try again or call the bank.
                error?.registrationId?.let { preferences.setSignupRegistrationId(it) }
                updateState {
                    it.copy(
                        isLoading = false,
                        error = CodeError.Message(error?.userMessage ?: result.message),
                        supportPhoneNumbers = error?.supportPhoneNumbers.orEmpty().ifEmpty { it.supportPhoneNumbers },
                    )
                }
            }
        }
    }

    private fun backToSignup(message: String?) {
        authHandoff.signupNotice = message
        sendEvent(VerifyCodeEvent.BackToSignup(message))
    }

    private suspend fun forgetSignup() {
        preferences.setSignupRegistrationId("")
    }

    private fun apiError(result: DataState.Error<*>): ApiError? = (result.exception as? ApiException)?.error

    /** Counts the code lifetime and the resend wait down once a second. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (state.expirySeconds > 0 || state.resendSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
                updateState {
                    // The tick that brings the lifetime to zero is the moment the code stops working.
                    val expiredNow = it.codeActive && it.expirySeconds == 1
                    it.copy(
                        expirySeconds = (it.expirySeconds - 1).coerceAtLeast(0),
                        resendSeconds = (it.resendSeconds - 1).coerceAtLeast(0),
                        codeActive = it.codeActive && !expiredNow,
                        error = if (expiredNow) CodeError.Expired else it.error,
                    )
                }
            }
        }
    }

    private fun updateState(update: (VerifyCodeState) -> VerifyCodeState) = mutableStateFlow.update(update)

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
        const val DEFAULT_RESEND_SECONDS = 60
    }
}

internal data class VerifyCodeState(
    val registrationId: String = "",
    val channel: String = CodeChannel.SMS,
    val destination: String = "",
    val smsFailed: Boolean = false,
    val code: String = "",
    val codeLength: Int = CODE_LENGTH,
    val codeActive: Boolean = true,
    val codeFromSms: Boolean = false,
    val expirySeconds: Int = 0,
    val resendSeconds: Int = 0,
    val attemptsLeft: Int? = null,
    val error: CodeError? = null,
    val supportPhoneNumbers: List<String> = emptyList(),
    val isLoading: Boolean = false,
    /** Increases each time a code is sent, so the SMS reader restarts for the new message. */
    val sendCount: Int = 0,
) {
    /** The email layout (screen C) is used when the code went, or had to go, by email. */
    val isEmail: Boolean get() = channel == CodeChannel.EMAIL

    val canVerify: Boolean get() = codeActive && code.length == codeLength && !isLoading
}

/** What went wrong with the code, kept as data so the screen can word it in the member's language. */
internal sealed interface CodeError {
    data class Wrong(val attemptsLeft: Int?) : CodeError
    data object Expired : CodeError
    data class Message(val text: String) : CodeError
}

internal sealed interface VerifyCodeEvent {
    data object NavigateToLogin : VerifyCodeEvent
    data object NavigateBack : VerifyCodeEvent

    /** The sign-up is gone (expired, replaced or never started), so start again from the form. */
    data class BackToSignup(val message: String?) : VerifyCodeEvent
}

internal sealed interface VerifyCodeAction {
    data class CodeChanged(val code: String) : VerifyCodeAction
    data class SmsCodeReceived(val code: String) : VerifyCodeAction
    data object Verify : VerifyCodeAction
    data class Resend(val channel: String) : VerifyCodeAction
    data object Back : VerifyCodeAction
}
