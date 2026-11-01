/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.model

/** Fineract error codes the app reacts to. */
object ApiErrorCodes {
    const val VIEW_ONLY = "error.msg.selfservice.user.view.only"
    const val TRANSFER_LIMIT_PER_TRANSFER = "error.msg.selfservice.transfer.limit.per.transfer.exceeded"
    const val TRANSFER_LIMIT_DAILY = "error.msg.selfservice.transfer.limit.daily.exceeded"
    const val SAVINGS_ACCOUNT_INVALID = "error.msg.saving.account.id.invalid"
    const val LOAN_INVALID = "error.msg.loan.id.invalid"
    const val SERVICE_DISABLED = "error.msg.selfservice.disabled"
    const val NOT_AUTHENTICATED = "error.msg.not.authenticated"
    const val USER_SUSPENDED = "error.msg.selfservice.user.suspended"
    const val USER_CLOSED = "error.msg.selfservice.user.closed"
    const val USER_LOCKED = "error.msg.selfservice.user.locked"
    const val USER_DISABLED = "error.msg.selfservice.user.disabled"
    const val USER_PENDING = "error.msg.selfservice.user.pending"
    const val PASSWORD_RESET_REQUIRED = "error.msg.selfservice.password.reset.required"
    const val INVALID_REQUEST = "error.msg.invalid.request"
}

/** Generic message for unexpected server errors; never shown together with a logout. */
const val GENERIC_SERVER_ERROR_MESSAGE = "Something went wrong. Please try again."

/**
 * A server error with its HTTP status and, when the body is a Fineract error, its code and message.
 * Plain (non-Fineract) bodies, for example the Spring 403 for an ended session, have a null [code].
 */
data class ApiError(
    val status: Int,
    val code: String?,
    val message: String?,
    val supportPhoneNumbers: List<String> = emptyList(),
    val shouldRenewPassword: Boolean = false,
    /** Temporary token sent with a forced password change (the session is not usable otherwise). */
    val authenticationKey: String? = null,
    val username: String? = null,
) {
    val isServerError: Boolean get() = status >= HTTP_SERVER_ERROR

    /**
     * What to show the user. Unexpected 5xx errors and unparseable bodies get the generic message;
     * the "app switched off" 503 keeps the server's own text.
     */
    val userMessage: String
        get() = when {
            code == ApiErrorCodes.SERVICE_DISABLED -> message ?: GENERIC_SERVER_ERROR_MESSAGE
            isServerError -> GENERIC_SERVER_ERROR_MESSAGE
            else -> message ?: GENERIC_SERVER_ERROR_MESSAGE
        }

    private companion object {
        const val HTTP_SERVER_ERROR = 500
    }
}

/** Carries a parsed [ApiError] through the repositories so screens can react to its code. */
class ApiException(val error: ApiError) : Exception(error.userMessage)
