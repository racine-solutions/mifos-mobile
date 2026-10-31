/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.datastore.model

/** Why the app ended the session; the login screen shows a message for it once. */
object SessionEndReason {
    /** The user was idle for longer than the allowed time. */
    const val IDLE = "idle"

    /** The server ended or revoked the session (expired, signed out elsewhere, replaced). */
    const val ENDED = "ended"
}
