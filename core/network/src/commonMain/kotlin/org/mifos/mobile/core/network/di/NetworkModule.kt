/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.http.HttpHeaders
import org.koin.dsl.module
import org.mifos.mobile.core.datastore.UserPreferencesRepository
import org.mifos.mobile.core.datastore.model.SessionEndReason
import org.mifos.mobile.core.network.DataManager
import org.mifos.mobile.core.network.KtorfitClient
import org.mifos.mobile.core.network.ktorHttpClient
import org.mifos.mobile.core.network.utils.BaseURL
import org.mifos.mobile.core.network.utils.KtorInterceptor
import org.mifos.mobile.core.network.utils.SessionImpact
import org.mifos.mobile.core.network.utils.isAuthenticationEndpoint
import org.mifos.mobile.core.network.utils.sessionImpact
import org.mifos.mobile.core.network.utils.sessionTimeoutMinutes
import org.mifos.mobile.core.network.utils.toApiError

val NetworkModule = module {

    single<HttpClient>(KtorClient) {
        val preferencesRepository = get<UserPreferencesRepository>()

        ktorHttpClient.config {
            install(Auth)
            install(KtorInterceptor) {
                getToken = { preferencesRepository.token.value }
                getTenant = { preferencesRepository.settingsInfo.value.tenant }
                onSessionTimeoutMinutes = { minutes ->
                    if (minutes != preferencesRepository.settingsInfo.value.sessionTimeoutMinutes) {
                        preferencesRepository.setSessionTimeoutMinutes(minutes)
                    }
                }
            }

            // One place that decides what an error on an authenticated call means for the session.
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, request ->
                    val response = (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                    response.sessionTimeoutMinutes()?.let { minutes ->
                        if (minutes != preferencesRepository.settingsInfo.value.sessionTimeoutMinutes) {
                            preferencesRepository.setSessionTimeoutMinutes(minutes)
                        }
                    }
                    val wasAuthenticated = request.headers.contains(HttpHeaders.Authorization)
                    if (!wasAuthenticated || isAuthenticationEndpoint(request.url.encodedPath)) {
                        return@handleResponseExceptionWithRequest
                    }
                    val apiError = response.toApiError()
                    val signedIn = preferencesRepository.settingsInfo.value.isAuthenticated
                    when (apiError.sessionImpact()) {
                        SessionImpact.SESSION_ENDED ->
                            if (signedIn) preferencesRepository.endSession(SessionEndReason.ENDED)
                        SessionImpact.SERVICE_DISABLED ->
                            if (signedIn) preferencesRepository.endSession(SessionEndReason.DISABLED)
                        SessionImpact.VIEW_ONLY ->
                            preferencesRepository.setAccessInfo(isViewOnly = true, reason = apiError.message.orEmpty())
                        SessionImpact.NONE -> Unit
                    }
                }
            }
        }
    }

    single<KtorfitClient>(MifosClient) {
        KtorfitClient.builder()
            .httpClient(get(KtorClient))
            .baseURL(BaseURL().url)
            .build()
    }

    single {
        DataManager(ktorfitClient = get(MifosClient))
    }
}
