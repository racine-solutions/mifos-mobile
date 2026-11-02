/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import io.ktor.client.statement.HttpResponse
import org.mifos.mobile.core.model.access.PasswordResetResendRequest
import org.mifos.mobile.core.model.access.PasswordResetStartRequest
import org.mifos.mobile.core.model.access.PasswordResetVerifyRequest
import org.mifos.mobile.core.model.access.SignupResendRequest
import org.mifos.mobile.core.model.access.SignupResumeRequest
import org.mifos.mobile.core.model.access.SignupStartRequest
import org.mifos.mobile.core.model.access.SignupVerifyRequest
import org.mifos.mobile.core.network.utils.ApiEndPoints

/** Public (no login) sign-up and password reset endpoints that work with a 6-digit code. */
interface SelfServiceAccessService {
    @POST(ApiEndPoints.SIGNUP + "/start")
    suspend fun signupStart(@Body request: SignupStartRequest): HttpResponse

    @POST(ApiEndPoints.SIGNUP + "/resend")
    suspend fun signupResend(@Body request: SignupResendRequest): HttpResponse

    @POST(ApiEndPoints.SIGNUP + "/resume")
    suspend fun signupResume(@Body request: SignupResumeRequest): HttpResponse

    @POST(ApiEndPoints.SIGNUP + "/verify")
    suspend fun signupVerify(@Body request: SignupVerifyRequest): HttpResponse

    @POST(ApiEndPoints.PASSWORD_RESET + "/start")
    suspend fun passwordResetStart(@Body request: PasswordResetStartRequest): HttpResponse

    @POST(ApiEndPoints.PASSWORD_RESET + "/resend")
    suspend fun passwordResetResend(@Body request: PasswordResetResendRequest): HttpResponse

    @POST(ApiEndPoints.PASSWORD_RESET + "/verify")
    suspend fun passwordResetVerify(@Body request: PasswordResetVerifyRequest): HttpResponse
}
