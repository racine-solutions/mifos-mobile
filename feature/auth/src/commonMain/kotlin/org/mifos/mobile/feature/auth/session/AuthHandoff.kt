/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.session

import org.mifos.mobile.core.model.access.CodeDelivery
import org.mifos.mobile.core.model.access.PasswordResetStarted

/**
 * In-memory hand-over between the sign-up, forgot-password and login screens. Nothing here is ever
 * written to disk, so a typed password or a code is gone when the app process ends.
 */
class AuthHandoff {
    /** Answer of sign-up start, shown by the code screen so it needs no second call. */
    var signupDelivery: CodeDelivery? = null

    /** Credentials typed on the sign-up form, kept only until the automatic login after verifying. */
    var signupUsername: String? = null
    var signupPassword: String? = null

    /** Answer of password-reset start, and what the member typed to ask for it. */
    var resetStarted: PasswordResetStarted? = null
    var resetIdentifier: String = ""

    /** Why the member was sent back to the sign-up form (for example the sign-up expired). */
    var signupNotice: String? = null

    /** Pre-fills the login form (and the forgot-password form) when set. */
    var loginUsername: String? = null

    /** A short message the login screen shows once, for example "Password changed". */
    var loginNotice: String? = null

    /** Log in straight away with these credentials once the login screen opens. */
    var autoLoginUsername: String? = null
    var autoLoginPassword: String? = null

    fun clearSignupSecrets() {
        signupUsername = null
        signupPassword = null
    }

    fun takeSignupDelivery(): CodeDelivery? = signupDelivery.also { signupDelivery = null }

    fun takeSignupNotice(): String? = signupNotice.also { signupNotice = null }

    fun takeResetStarted(): PasswordResetStarted? = resetStarted.also { resetStarted = null }

    fun takeLoginUsername(): String? = loginUsername.also { loginUsername = null }

    fun takeLoginNotice(): String? = loginNotice.also { loginNotice = null }

    /** Returns the pending automatic login once, then forgets the password. */
    fun takeAutoLogin(): Pair<String, String>? {
        val username = autoLoginUsername
        val password = autoLoginPassword
        autoLoginUsername = null
        autoLoginPassword = null
        return if (username != null && password != null) username to password else null
    }
}
