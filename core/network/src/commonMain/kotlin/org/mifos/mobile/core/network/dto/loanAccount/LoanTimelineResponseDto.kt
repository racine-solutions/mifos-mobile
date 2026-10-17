/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.dto.loanAccount

import kotlinx.serialization.Serializable
import org.mifos.mobile.core.network.dto.common.FlexibleDateListSerializer

@Serializable
data class LoanTimelineResponseDto(
    @Serializable(with = FlexibleDateListSerializer::class)
    val submittedOnDate: List<Int>? = null,

    val submittedByUsername: String?,

    val submittedByFirstname: String?,

    val submittedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val approvedOnDate: List<Int>? = null,

    val approvedByUsername: String?,

    val approvedByFirstname: String?,

    val approvedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val expectedDisbursementDate: List<Int>? = null,

    @Serializable(with = FlexibleDateListSerializer::class)
    val actualDisbursementDate: List<Int>? = null,

    val disbursedByUsername: String?,

    val disbursedByFirstname: String?,

    val disbursedByLastname: String?,

    @Serializable(with = FlexibleDateListSerializer::class)
    val closedOnDate: List<Int>? = null,

    @Serializable(with = FlexibleDateListSerializer::class)
    val expectedMaturityDate: List<Int>? = null,

    @Serializable(with = FlexibleDateListSerializer::class)
    val withdrawnOnDate: List<Int>? = null,

)
