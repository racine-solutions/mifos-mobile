/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import mifos_mobile.feature.home.generated.resources.Res
import mifos_mobile.feature.home.generated.resources.feature_home_branch
import mifos_mobile.feature.home.generated.resources.feature_home_signed_in_to
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifos.mobile.core.designsystem.icon.MifosIcons
import org.mifos.mobile.core.designsystem.theme.MifosMobileTheme
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import template.core.base.designsystem.theme.KptTheme

/**
 * Shows the organisation (Sacco/MFI tenant) the user is signed in to and the branch they belong to,
 * so users can confirm they are in the right institution.
 *
 * @param tenantName Name of the tenant returned by the backend on login.
 * @param branchName Name of the user's branch (office). The branch row is hidden when blank.
 */
@Composable
internal fun OrganizationCard(
    tenantName: String,
    branchName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KptTheme.shapes.large)
            .background(KptTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .border(
                width = KptTheme.spacing.xs / 4,
                color = KptTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = KptTheme.shapes.large,
            )
            .padding(KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(KptTheme.spacing.xl + KptTheme.spacing.md)
                .clip(CircleShape)
                .background(KptTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MifosIcons.AccountBalance,
                contentDescription = null,
                tint = KptTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.feature_home_signed_in_to),
                style = MifosTypography.labelSmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tenantName,
                style = MifosTypography.titleMediumEmphasized,
                color = KptTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (branchName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
                ) {
                    Icon(
                        imageVector = MifosIcons.LocationOn,
                        contentDescription = null,
                        tint = KptTheme.colorScheme.primary,
                        modifier = Modifier.size(KptTheme.spacing.md),
                    )
                    Text(
                        text = stringResource(Res.string.feature_home_branch, branchName),
                        style = MifosTypography.bodySmall,
                        color = KptTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OrganizationCardPreview() {
    MifosMobileTheme {
        OrganizationCard(
            tenantName = "Racine Demo SACCO",
            branchName = "Kampala Main Branch",
            modifier = Modifier.padding(KptTheme.spacing.md),
        )
    }
}
