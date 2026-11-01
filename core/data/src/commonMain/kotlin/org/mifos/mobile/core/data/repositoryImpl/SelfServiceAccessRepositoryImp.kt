/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.data.repositoryImpl

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.data.repository.SelfServiceAccessRepository
import org.mifos.mobile.core.model.ApiException
import org.mifos.mobile.core.model.access.CodeDelivery
import org.mifos.mobile.core.model.access.PasswordResetDone
import org.mifos.mobile.core.model.access.PasswordResetResendRequest
import org.mifos.mobile.core.model.access.PasswordResetStartRequest
import org.mifos.mobile.core.model.access.PasswordResetStarted
import org.mifos.mobile.core.model.access.PasswordResetVerifyRequest
import org.mifos.mobile.core.model.access.SignupResendRequest
import org.mifos.mobile.core.model.access.SignupResume
import org.mifos.mobile.core.model.access.SignupResumeRequest
import org.mifos.mobile.core.model.access.SignupStartRequest
import org.mifos.mobile.core.model.access.SignupVerified
import org.mifos.mobile.core.model.access.SignupVerifyRequest
import org.mifos.mobile.core.network.DataManager
import org.mifos.mobile.core.network.utils.toApiError

class SelfServiceAccessRepositoryImp(
    private val dataManager: DataManager,
    private val ioDispatcher: CoroutineDispatcher,
) : SelfServiceAccessRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val api get() = dataManager.selfServiceAccessApi

    override suspend fun signupStart(request: SignupStartRequest): DataState<CodeDelivery> =
        call<CodeDelivery> { api.signupStart(request) }

    override suspend fun signupResend(registrationId: String, channel: String): DataState<CodeDelivery> =
        call<CodeDelivery> { api.signupResend(SignupResendRequest(registrationId, channel)) }

    override suspend fun signupResume(registrationId: String): DataState<SignupResume> =
        call<SignupResume> { api.signupResume(SignupResumeRequest(registrationId)) }

    override suspend fun signupVerify(registrationId: String, code: String): DataState<SignupVerified> =
        call<SignupVerified> { api.signupVerify(SignupVerifyRequest(registrationId, code)) }

    override suspend fun passwordResetStart(identifier: String): DataState<PasswordResetStarted> =
        call<PasswordResetStarted> { api.passwordResetStart(PasswordResetStartRequest(identifier)) }

    override suspend fun passwordResetResend(resetId: String): DataState<PasswordResetStarted> =
        call<PasswordResetStarted> { api.passwordResetResend(PasswordResetResendRequest(resetId)) }

    override suspend fun passwordResetVerify(
        resetId: String,
        code: String,
        password: String,
        repeatPassword: String,
    ): DataState<PasswordResetDone> = call<PasswordResetDone> {
        api.passwordResetVerify(PasswordResetVerifyRequest(resetId, code, password, repeatPassword))
    }

    private suspend inline fun <reified T> call(crossinline request: suspend () -> HttpResponse): DataState<T> =
        withContext(ioDispatcher) {
            try {
                DataState.Success(json.decodeFromString<T>(request().bodyAsText()))
            } catch (e: ClientRequestException) {
                DataState.Error(ApiException(e.response.toApiError()), null)
            } catch (e: ServerResponseException) {
                DataState.Error(ApiException(e.response.toApiError()), null)
            } catch (e: IOException) {
                DataState.Error(Exception("Network error", e), null)
            }
        }
}
