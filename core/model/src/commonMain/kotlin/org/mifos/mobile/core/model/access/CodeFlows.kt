/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.model.access

import kotlinx.serialization.Serializable

/** How a one-time code travels to the member. */
object CodeChannel {
    const val SMS = "SMS"
    const val EMAIL = "EMAIL"
}

/** Number of digits in every one-time code. */
const val CODE_LENGTH = 6

@Serializable
data class SignupStartRequest(
    val accountNumber: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val email: String,
    val password: String,
    val repeatPassword: String,
    val channel: String = CodeChannel.SMS,
)

@Serializable
data class SignupResendRequest(
    val registrationId: String,
    val channel: String,
)

@Serializable
data class SignupResumeRequest(val registrationId: String)

@Serializable
data class SignupVerifyRequest(
    val registrationId: String,
    val code: String,
)

/** Answer to sign-up start and resend: where the code went and how long it stays useful. */
@Serializable
data class CodeDelivery(
    val registrationId: String,
    val channel: String = CodeChannel.SMS,
    val destination: String = "",
    val smsFailed: Boolean = false,
    val codeLength: Int = CODE_LENGTH,
    val codeExpiresInSeconds: Int = 600,
    val resendAfterSeconds: Int = 60,
)

@Serializable
data class SignupResume(
    val registrationId: String,
    val channel: String = CodeChannel.SMS,
    val destination: String = "",
    val codeLength: Int = CODE_LENGTH,
    val codeActive: Boolean = true,
    val codeExpiresInSeconds: Int = 0,
    val attemptsLeft: Int = 0,
    val resendAfterSeconds: Int = 0,
)

@Serializable
data class SignupVerified(
    val userId: Long = 0,
    val username: String = "",
    val clientId: Long = 0,
)

@Serializable
data class PasswordResetStartRequest(val identifier: String)

@Serializable
data class PasswordResetResendRequest(val resetId: String)

@Serializable
data class PasswordResetVerifyRequest(
    val resetId: String,
    val code: String,
    val password: String,
    val repeatPassword: String,
)

/** The answer is always the same, whether or not an account matched, so it carries no destination. */
@Serializable
data class PasswordResetStarted(
    val resetId: String = "",
    val codeLength: Int = CODE_LENGTH,
    val codeExpiresInSeconds: Int = 600,
    val resendAfterSeconds: Int = 60,
    val message: String = "",
)

@Serializable
data class PasswordResetDone(val username: String = "")
