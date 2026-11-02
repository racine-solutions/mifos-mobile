/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.data.repository

import org.mifos.mobile.core.common.DataState
import org.mifos.mobile.core.model.access.CodeDelivery
import org.mifos.mobile.core.model.access.PasswordResetDone
import org.mifos.mobile.core.model.access.PasswordResetStarted
import org.mifos.mobile.core.model.access.SignupResume
import org.mifos.mobile.core.model.access.SignupStartRequest
import org.mifos.mobile.core.model.access.SignupVerified

/**
 * Sign-up and password reset with a 6-digit code. Errors carry an `ApiException`, so screens can
 * react to the server's error code.
 */
interface SelfServiceAccessRepository {
    suspend fun signupStart(request: SignupStartRequest): DataState<CodeDelivery>

    suspend fun signupResend(registrationId: String, channel: String): DataState<CodeDelivery>

    suspend fun signupResume(registrationId: String): DataState<SignupResume>

    suspend fun signupVerify(registrationId: String, code: String): DataState<SignupVerified>

    suspend fun passwordResetStart(identifier: String): DataState<PasswordResetStarted>

    suspend fun passwordResetResend(resetId: String): DataState<PasswordResetStarted>

    suspend fun passwordResetVerify(
        resetId: String,
        code: String,
        password: String,
        repeatPassword: String,
    ): DataState<PasswordResetDone>
}
