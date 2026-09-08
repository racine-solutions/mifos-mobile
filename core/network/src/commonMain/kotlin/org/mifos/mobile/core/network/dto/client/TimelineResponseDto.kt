/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.dto.client

import kotlinx.serialization.Serializable
import org.mifos.mobile.core.network.dto.common.FlexibleDateListSerializer

@Serializable
data class TimelineResponseDto(
    @Serializable(with = FlexibleDateListSerializer::class)
    val submittedOnDate: List<Int> = emptyList(),
    val submittedByUsername: String? = null,
    val submittedByFirstname: String? = null,
    val submittedByLastname: String? = null,
    @Serializable(with = FlexibleDateListSerializer::class)
    val activatedOnDate: List<Int> = emptyList(),
    val activatedByUsername: String? = null,
    val activatedByFirstname: String? = null,
    val activatedByLastname: String? = null,
    @Serializable(with = FlexibleDateListSerializer::class)
    val closedOnDate: List<Int> = emptyList(),
    val closedByUsername: String? = null,
    val closedByFirstname: String? = null,
    val closedByLastname: String? = null,
)
