/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import org.mifos.mobile.core.datastore.UserPreferencesRepository

class KtorInterceptor(
    private val getToken: () -> String?,
    private val getTenant: () -> String,
    private val onUnauthorized: (suspend () -> Unit)?,
    private val onSessionTimeoutMinutes: (suspend (Int) -> Unit)? = null,
) {
    companion object Plugin : HttpClientPlugin<Config, KtorInterceptor> {
        private const val HEADER_TENANT = "Fineract-Platform-TenantId"
        private const val HEADER_AUTH = "Authorization"
        private const val CONTENT_TYPE = "Content-Type"
        override val key: AttributeKey<KtorInterceptor> = AttributeKey("KtorInterceptor")

        override fun install(plugin: KtorInterceptor, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.header(CONTENT_TYPE, "application/json")
                context.header("Accept", "application/json")
                context.header("Accept", "*/*")
                plugin.getTenant().takeIf { it.isNotBlank() }?.let { tenant ->
                    context.header(HEADER_TENANT, tenant)
                }

                plugin.getToken()?.let { token ->
                    if (token.isNotEmpty()) {
                        context.headers[HEADER_AUTH] = "Basic $token"
                    }
                }
            }
            scope.responsePipeline.intercept(HttpResponsePipeline.After) {
                context.response.sessionTimeoutMinutes()?.let { minutes ->
                    runCatching { plugin.onSessionTimeoutMinutes?.invoke(minutes) }
                }
                if (context.response.status == HttpStatusCode.Unauthorized) {
                    runCatching {
                        plugin.onUnauthorized?.invoke()
                    }.onFailure { throwable ->
                        throwable.printStackTrace()
                    }
                }
                proceed()
            }
        }

        override fun prepare(block: Config.() -> Unit): KtorInterceptor {
            val config = Config().apply(block)
            return KtorInterceptor(
                getToken = config.getToken,
                getTenant = config.getTenant,
                onUnauthorized = config.onUnauthorized,
                onSessionTimeoutMinutes = config.onSessionTimeoutMinutes,
            )
        }
    }
}

class Config {
    lateinit var getToken: () -> String?
    var getTenant: () -> String = { "" }
    var onUnauthorized: (suspend () -> Unit)? = null
    var onSessionTimeoutMinutes: (suspend (Int) -> Unit)? = null
}

class KtorInterceptorRe(
    private val repository: UserPreferencesRepository,
) {
    companion object Plugin : HttpClientPlugin<ConfigRe, KtorInterceptorRe> {
        private const val HEADER_TENANT = "Fineract-Platform-TenantId"
        private const val HEADER_AUTH = "Authorization"
        private const val CONTENT_TYPE = "Content-Type"
        override val key: AttributeKey<KtorInterceptorRe> = AttributeKey("KtorInterceptorRe")

        override fun install(plugin: KtorInterceptorRe, scope: HttpClient) {
            val token = plugin.repository.token.value

            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.header(CONTENT_TYPE, "application/json")
                context.header("Accept", "application/json")
                plugin.repository.settingsInfo.value.tenant.takeIf { it.isNotBlank() }?.let { tenant ->
                    context.header(HEADER_TENANT, tenant)
                }

                token?.let { token ->
                    if (token.isNotEmpty()) {
                        context.headers[HEADER_AUTH] = "Basic $token"
                    }
                }
            }
        }

        override fun prepare(block: ConfigRe.() -> Unit): KtorInterceptorRe {
            val config = ConfigRe().apply(block)
            return KtorInterceptorRe(config.repository)
        }
    }
}

class ConfigRe {
    lateinit var repository: UserPreferencesRepository
}
