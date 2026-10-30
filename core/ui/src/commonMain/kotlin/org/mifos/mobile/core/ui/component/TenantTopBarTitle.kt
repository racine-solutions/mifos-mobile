/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import template.core.base.designsystem.theme.KptTheme

/**
 * Top bar title for the main tab screens (Home, Transfer): the organisation's badge and name, with the branch below,
 * so the institution (not the platform) leads.
 */
@Composable
fun TenantTopBarTitle(
    tenantName: String,
    branchName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        TenantBadge(
            name = tenantName,
            size = KptTheme.spacing.xl,
            textStyle = MifosTypography.labelLarge,
        )
        Column {
            Text(
                text = tenantName,
                style = MifosTypography.titleSmallEmphasized,
                color = KptTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (branchName.isNotBlank()) {
                Text(
                    text = branchName,
                    style = MifosTypography.labelSmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
