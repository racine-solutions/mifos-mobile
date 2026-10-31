/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_auth_login_incorrect
import mifos_mobile.feature.auth.generated.resources.feature_auth_password_reset_required
import mifos_mobile.feature.auth.generated.resources.feature_auth_service_disabled
import mifos_mobile.feature.auth.generated.resources.feature_auth_session_ended
import mifos_mobile.feature.auth.generated.resources.feature_auth_session_idle
import mifos_mobile.feature.auth.generated.resources.feature_auth_signed_out_title
import mifos_mobile.feature.auth.generated.resources.feature_sign_in_password_error
import mifos_mobile.feature.auth.generated.resources.feature_sign_in_username_error
import mifos_mobile.feature.auth.generated.resources.no_client_assigned
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.data.repository.UserAuthRepository
import org.mifos.mobile.core.datastore.UserPreferencesRepository
import org.mifos.mobile.core.datastore.model.SessionEndReason
import org.mifos.mobile.core.datastore.model.UserData
import org.mifos.mobile.core.model.ApiError
import org.mifos.mobile.core.model.ApiErrorCodes
import org.mifos.mobile.core.model.ApiException
import org.mifos.mobile.core.model.entity.User
import org.mifos.mobile.core.ui.utils.BaseViewModel
import org.mifos.mobile.core.ui.utils.ScreenUiState

class LoginViewModel(
    private val userAuthRepositoryImpl: UserAuthRepository,
    private val userPreferencesRepositoryImpl: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginState, LoginEvent, LoginAction>(
    initialState = LoginState(
        uiState = ScreenUiState.Success,
        showTenantField = userPreferencesRepositoryImpl.settingsInfo.value.tenant.isBlank(),
        tenantDisplayName = userPreferencesRepositoryImpl.settingsInfo.value.let {
            it.tenantName.ifBlank { it.tenant }
        },
    ),
) {

    private var loginJob: Job? = null

    init {
        // The Sacco/MFI code is only asked for while none is stored in the datastore.
        userPreferencesRepositoryImpl.settingsInfo
            .onEach { settings ->
                showSessionEndedMessage(settings.sessionEndedReason, settings.sessionTimeoutMinutes)
                updateState {
                    it.copy(
                        showTenantField = settings.tenant.isBlank(),
                        tenantDisplayName = settings.tenantName.ifBlank { settings.tenant },
                    )
                }
            }
            .launchIn(viewModelScope)

        savedStateHandle.get<String>("username")?.let {
            trySendAction(LoginAction.UsernameChanged(it))
        }
    }

    /** Tells the user once why they are back on the login screen, then forgets the reason. */
    private fun showSessionEndedMessage(reason: String, timeoutMinutes: Int) {
        if (reason.isEmpty()) return
        viewModelScope.launch {
            val message = when (reason) {
                SessionEndReason.IDLE -> getString(Res.string.feature_auth_session_idle, timeoutMinutes)
                SessionEndReason.DISABLED -> getString(Res.string.feature_auth_service_disabled)
                else -> getString(Res.string.feature_auth_session_ended)
            }
            userPreferencesRepositoryImpl.clearSessionEndedReason()
            val title = getString(Res.string.feature_auth_signed_out_title)
            updateState { it.copy(dialogState = LoginState.DialogState.Notice(title, message)) }
        }
    }

    private fun updateState(update: (LoginState) -> LoginState) {
        mutableStateFlow.update(update)
    }

    override fun handleAction(action: LoginAction) {
        when (action) {
            is LoginAction.TenantCodeChanged -> {
                updateState {
                    it.copy(
                        isError = false,
                        tenantCode = action.tenantCode.trim(),
                    )
                }
            }

            is LoginAction.ChangeTenantClicked -> {
                viewModelScope.launch {
                    userPreferencesRepositoryImpl.setTenant("")
                }
                updateState { it.copy(tenantCode = "") }
            }

            is LoginAction.UsernameChanged -> {
                updateState {
                    it.copy(
                        isError = false,
                        username = action.username,
                        userNameError = null,
                    )
                }
            }

            is LoginAction.PasswordChanged -> {
                updateState {
                    it.copy(
                        isError = false,
                        password = action.password,
                        passwordError = null,
                    )
                }
            }

            is LoginAction.TogglePasswordVisibility -> {
                updateState { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is LoginAction.LoginClicked -> loginUser(state.username, state.password)

            is LoginAction.Internal.ReceiveLoginResult -> handleLoginResult(action)

            is LoginAction.SignupClicked -> sendEvent(LoginEvent.NavigateToSignup)

            is LoginAction.NavigateToForgotPassword -> sendEvent(LoginEvent.NavigateToForgotPassword)

            is LoginAction.ErrorDialogDismiss -> {
                val dialog = state.dialogState
                updateState { it.copy(dialogState = null) }
                if (dialog is LoginState.DialogState.Refused) {
                    when (dialog.next) {
                        LoginState.NextStep.CONFIRM_CODE -> sendEvent(LoginEvent.NavigateToConfirmCode)
                        LoginState.NextStep.SET_PASSWORD -> sendEvent(LoginEvent.NavigateToSetPassword)
                        LoginState.NextStep.NONE -> Unit
                    }
                }
            }
        }
    }

    private fun handleLoginResult(action: LoginAction.Internal.ReceiveLoginResult) {
        viewModelScope.launch {
            when (action.loginResult) {
                is DataState.Error -> {
                    val apiError = (action.loginResult.exception as? ApiException)?.error
                    if (apiError != null && handleRefusedLogin(apiError)) return@launch
                    // Only a genuine credentials failure marks the fields as wrong; outages and
                    // network problems say nothing about what the user typed.
                    val badCredentials = apiError?.code == ApiErrorCodes.NOT_AUTHENTICATED
                    val errorMsg = if (badCredentials) {
                        getString(Res.string.feature_auth_login_incorrect)
                    } else {
                        action.loginResult.message
                    }

                    updateState {
                        it.copy(
                            isError = true,
                            uiState = ScreenUiState.Success,
                            showOverlay = false,
                            dialogState = LoginState.DialogState.Error(errorMsg),
                            userNameError = if (badCredentials) Res.string.feature_sign_in_username_error else null,
                            passwordError = if (badCredentials) Res.string.feature_sign_in_password_error else null,
                        )
                    }
                }

                is DataState.Loading -> {
                    updateState { it.copy(showOverlay = true) }
                }

                is DataState.Success -> {
                    updateState { it.copy(showOverlay = false) }
                    val user = action.loginResult.data
                    if (user.clients.isEmpty()) {
                        val noClientsMsg = getString(Res.string.no_client_assigned)
                        viewModelScope.launch {
                            userPreferencesRepositoryImpl.updateUser(UserData.DEFAULT)
                            userPreferencesRepositoryImpl.setIsAuthenticated(false)
                        }
                        updateState {
                            it.copy(
                                isError = true,
                                dialogState = LoginState.DialogState.Error(noClientsMsg),
                            )
                        }
                    } else {
                        val userData = UserData(
                            userId = user.userId,
                            userName = user.username.orEmpty(),
                            clientId = user.clients[0],
                            isAuthenticated = user.isAuthenticated,
                            base64EncodedAuthenticationKey = user.base64EncodedAuthenticationKey.orEmpty(),
                            officeName = user.officeName.orEmpty(),
                            tenantName = user.tenantName.orEmpty(),
                            password = state.password,
                        )
                        viewModelScope.launch {
                            userPreferencesRepositoryImpl.setTenantName(user.tenantName.orEmpty())
                            user.sessionTimeoutMinutes?.let { userPreferencesRepositoryImpl.setSessionTimeoutMinutes(it) }
                            userPreferencesRepositoryImpl.updateUser(userData)
                            userPreferencesRepositoryImpl.setAccessInfo(
                                isViewOnly = user.accessStatus == ACCESS_VIEW_ONLY,
                                reason = user.accessReason.orEmpty(),
                                phones = user.supportPhoneNumbers,
                            )
                            userPreferencesRepositoryImpl.setIsAuthenticated(true)
                        }
                        sendEvent(LoginEvent.NavigateToPasscode)
                    }
                }
            }
        }
    }

    /**
     * Shows the server's explanation for a login the bank refused (suspended, closed, locked,
     * disabled, pending or password reset required). Returns false for ordinary failures.
     */
    private suspend fun handleRefusedLogin(error: ApiError): Boolean {
        val next = when (error.code) {
            ApiErrorCodes.USER_SUSPENDED,
            ApiErrorCodes.USER_CLOSED,
            ApiErrorCodes.USER_LOCKED,
            ApiErrorCodes.USER_DISABLED,
            -> LoginState.NextStep.NONE
            ApiErrorCodes.USER_PENDING -> LoginState.NextStep.CONFIRM_CODE
            ApiErrorCodes.PASSWORD_RESET_REQUIRED -> LoginState.NextStep.SET_PASSWORD
            else -> return false
        }
        if (next == LoginState.NextStep.SET_PASSWORD) {
            // The server sends a temporary token that is only good for changing the password.
            error.authenticationKey?.let { key ->
                userPreferencesRepositoryImpl.updateUser(
                    UserData.DEFAULT.copy(userName = error.username.orEmpty(), base64EncodedAuthenticationKey = key),
                )
            }
        }
        val message = if (next == LoginState.NextStep.SET_PASSWORD) {
            getString(Res.string.feature_auth_password_reset_required)
        } else {
            error.userMessage
        }
        updateState {
            it.copy(
                showOverlay = false,
                uiState = ScreenUiState.Success,
                dialogState = LoginState.DialogState.Refused(message, error.supportPhoneNumbers, next),
            )
        }
        return true
    }

    private fun loginUser(
        username: String,
        password: String,
    ) {
        loginJob?.cancel()

        updateState { it.copy(showOverlay = true) }

        loginJob = viewModelScope.launch {
            delay(300)

            // The tenant header of every API call is read from the datastore, so persist the
            // Sacco/MFI code before hitting the backend.
            if (state.showTenantField) {
                userPreferencesRepositoryImpl.setTenant(state.tenantCode)
            }

            val result = userAuthRepositoryImpl.login(username, password)
            sendAction(LoginAction.Internal.ReceiveLoginResult(result))
        }
    }
}

data class LoginState(
    val tenantCode: String = "",
    val showTenantField: Boolean = false,
    val tenantDisplayName: String = "",
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val clientName: String = "",
    val isError: Boolean = false,
    val userNameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val dialogState: DialogState? = null,
    val uiState: ScreenUiState?,
    val showOverlay: Boolean = false,
) {
    /** Where to go after the user reads a refusal message. */
    enum class NextStep { NONE, CONFIRM_CODE, SET_PASSWORD }

    sealed interface DialogState {
        data class Error(val message: String) : DialogState
        data class Notice(val title: String, val message: String) : DialogState
        data class Refused(
            val message: String,
            val phoneNumbers: List<String>,
            val next: NextStep,
        ) : DialogState
    }

    val isLoginButtonEnabled: Boolean
        get() = (!showTenantField || tenantCode.isNotBlank()) && username.isNotEmpty() && password.length >= 8
}

sealed interface LoginEvent {
    data object NavigateToSignup : LoginEvent
    data object NavigateToPasscode : LoginEvent
    data object NavigateToForgotPassword : LoginEvent
    data object NavigateToConfirmCode : LoginEvent
    data object NavigateToSetPassword : LoginEvent
    data class ShowToast(val message: String) : LoginEvent
}

sealed interface LoginAction {
    data class TenantCodeChanged(val tenantCode: String) : LoginAction
    data object ChangeTenantClicked : LoginAction
    data class UsernameChanged(val username: String) : LoginAction
    data class PasswordChanged(val password: String) : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object ErrorDialogDismiss : LoginAction
    data object LoginClicked : LoginAction
    data object SignupClicked : LoginAction
    data object NavigateToForgotPassword : LoginAction

    sealed class Internal : LoginAction {
        data class ReceiveLoginResult(
            val loginResult: DataState<User>,
        ) : Internal()
    }
}

private const val ACCESS_VIEW_ONLY = "VIEW_ONLY"
