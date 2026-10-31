/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.data.util

import io.ktor.client.statement.HttpResponse
import org.mifos.mobile.core.network.utils.toApiError

/**
 * Generic function to extract error messages from API responses. Unexpected server errors (5xx)
 * and unreadable bodies return a generic message instead of raw server text.
 */
suspend fun extractErrorMessage(response: HttpResponse): String = response.toApiError().userMessage
