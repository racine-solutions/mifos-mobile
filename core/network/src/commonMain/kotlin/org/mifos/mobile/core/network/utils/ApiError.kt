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

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mifos.mobile.core.model.ApiError
import org.mifos.mobile.core.model.ApiErrorCodes

@Serializable
private data class ApiErrorBody(
    val userMessageGlobalisationCode: String? = null,
    val defaultUserMessage: String? = null,
    val errors: List<ApiErrorItem> = emptyList(),
    val supportPhoneNumbers: List<String> = emptyList(),
    val shouldRenewPassword: Boolean? = null,
    val base64EncodedAuthenticationKey: String? = null,
    val username: String? = null,
)

@Serializable
private data class ApiErrorItem(
    val userMessageGlobalisationCode: String? = null,
    val defaultUserMessage: String? = null,
)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Reads the status and (if present) the Fineract code and message of an error response. */
suspend fun HttpResponse.toApiError(): ApiError {
    val body = runCatching { errorJson.decodeFromString<ApiErrorBody>(bodyAsText()) }.getOrNull()
    val first = body?.errors?.firstOrNull()
    return ApiError(
        status = status.value,
        code = body?.userMessageGlobalisationCode ?: first?.userMessageGlobalisationCode,
        message = first?.defaultUserMessage ?: body?.defaultUserMessage,
        supportPhoneNumbers = body?.supportPhoneNumbers.orEmpty(),
        shouldRenewPassword = body?.shouldRenewPassword == true,
        authenticationKey = body?.base64EncodedAuthenticationKey,
        username = body?.username,
    )
}

/** Every /self response carries the current idle timeout, so a staff change applies without a new login. */
const val SESSION_TIMEOUT_HEADER = "X-Session-Timeout-Minutes"

fun HttpResponse.sessionTimeoutMinutes(): Int? =
    headers[SESSION_TIMEOUT_HEADER]?.trim()?.toIntOrNull()?.takeIf { it > 0 }

/** How an error on an authenticated call should affect the session. */
enum class SessionImpact { NONE, SESSION_ENDED, VIEW_ONLY, SERVICE_DISABLED }

/**
 * 401 always means the session ended. A 403 with the view-only code is view-only mode. A 403 with
 * another Fineract code (for example a transfer limit) is a normal business error, while a 403
 * without any Fineract code is the plain Spring response for an expired or revoked token.
 */
fun ApiError.sessionImpact(): SessionImpact = when {
    status == HTTP_SERVICE_UNAVAILABLE && code == ApiErrorCodes.SERVICE_DISABLED -> SessionImpact.SERVICE_DISABLED
    status == HTTP_UNAUTHORIZED -> SessionImpact.SESSION_ENDED
    status == HTTP_FORBIDDEN && code == ApiErrorCodes.VIEW_ONLY -> SessionImpact.VIEW_ONLY
    status == HTTP_FORBIDDEN && code == null -> SessionImpact.SESSION_ENDED
    else -> SessionImpact.NONE
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val HTTP_FORBIDDEN = 403

/** Login, refresh and logout answer 401/403 for reasons that are not an ended session. */
fun isAuthenticationEndpoint(path: String): Boolean = path.contains("/authentication")
