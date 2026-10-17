/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.dto.savingsAccount

import kotlinx.serialization.Serializable
import org.mifos.mobile.core.network.dto.common.FlexibleDateListSerializer

@Serializable
data class SavingsTimeLineResponseDto(
    @Serializable(with = FlexibleDateListSerializer::class)
    val submittedOnDate: List<Int> = emptyList(),

    val submittedByUsername: String?,

    val submittedByFirstname: String?,

    val submittedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val approvedOnDate: List<Int> = emptyList(),

    val approvedByUsername: String?,

    val approvedByFirstname: String?,

    val approvedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val activatedOnDate: List<Int>? = null,

    val activatedByUsername: String?,

    val activatedByFirstname: String?,

    val activatedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val closedOnDate: List<Int> = emptyList(),

)
