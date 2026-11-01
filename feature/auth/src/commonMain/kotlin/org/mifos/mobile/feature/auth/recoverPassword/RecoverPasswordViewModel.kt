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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.data.repository.SelfServiceAccessRepository
import org.mifos.mobile.core.ui.utils.BaseViewModel
import org.mifos.mobile.core.ui.utils.ScreenUiState
import org.mifos.mobile.feature.auth.session.AuthHandoff

/** Longest phone number accepted by the legacy upload-ID form. */
const val PHONE_NUMBER_LENGTH = 10

/**
 * First step of "forgot password": one field (email, username or phone) and a code sent to the
 * phone or email the SACCO holds. The answer is always neutral, so this screen never reveals
 * whether an account exists.
 */
internal class RecoverPasswordViewModel(
    private val repository: SelfServiceAccessRepository,
    private val authHandoff: AuthHandoff,
) : BaseViewModel<RecoverPasswordState, RecoverPasswordEvent, RecoverPasswordAction>(
    initialState = RecoverPasswordState(identifier = authHandoff.resetIdentifier),
) {

    override fun handleAction(action: RecoverPasswordAction) {
        when (action) {
            RecoverPasswordAction.OnDismissDialog -> mutableStateFlow.update { it.copy(dialogState = null) }

            is RecoverPasswordAction.OnIdentifierChange -> mutableStateFlow.update { it.copy(identifier = action.identifier) }

            RecoverPasswordAction.OnLogin -> sendEvent(RecoverPasswordEvent.NavigateToLogin)

            RecoverPasswordAction.OnSendCode -> sendCode()
        }
    }

    private fun sendCode() {
        val identifier = state.identifier.trim()
        if (identifier.isEmpty() || state.showOverlay) return
        mutableStateFlow.update { it.copy(showOverlay = true) }
        viewModelScope.launch {
            when (val result = repository.passwordResetStart(identifier)) {
                is DataState.Success -> {
                    authHandoff.resetIdentifier = identifier
                    authHandoff.resetStarted = result.data
                    mutableStateFlow.update { it.copy(showOverlay = false) }
                    sendEvent(RecoverPasswordEvent.NavigateToResetPassword)
                }

                is DataState.Error -> mutableStateFlow.update {
                    it.copy(showOverlay = false, dialogState = RecoverPasswordState.DialogState.Error(result.message))
                }

                DataState.Loading -> Unit
            }
        }
    }
}

internal data class RecoverPasswordState(
    val identifier: String = "",
    val dialogState: DialogState? = null,
    val uiState: ScreenUiState? = ScreenUiState.Success,
    val showOverlay: Boolean = false,
) {
    sealed interface DialogState {
        data class Error(val message: String) : DialogState
    }

    val isSendEnabled: Boolean
        get() = identifier.isNotBlank() && !showOverlay
}

internal sealed interface RecoverPasswordEvent {
    data object NavigateToResetPassword : RecoverPasswordEvent
    data object NavigateToLogin : RecoverPasswordEvent
}

internal sealed interface RecoverPasswordAction {
    data class OnIdentifierChange(val identifier: String) : RecoverPasswordAction
    data object OnLogin : RecoverPasswordAction
    data object OnSendCode : RecoverPasswordAction
    data object OnDismissDialog : RecoverPasswordAction
}
