/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_auth_change_tenant
import mifos_mobile.feature.auth.generated.resources.feature_auth_signing_in_to
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifos.mobile.core.designsystem.theme.MifosMobileTheme
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.ui.component.TenantBadge
import template.core.base.designsystem.theme.KptTheme

/**
 * Shows which Sacco/MFI the user is about to sign in to: an initials badge, the organisation name
 * and a link to switch to another organisation.
 *
 * @param tenantName Display name of the organisation (falls back to the stored code).
 * @param onChangeClick Invoked when the user wants to enter a different Sacco/MFI code.
 */
@Composable
internal fun TenantHeaderCard(
    tenantName: String,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KptTheme.spacing.md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KptTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .border(KptTheme.spacing.xs / 4, KptTheme.colorScheme.primary.copy(alpha = 0.18f), shape)
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            TenantBadge(name = tenantName, size = KptTheme.spacing.xl + KptTheme.spacing.md)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.feature_auth_signing_in_to),
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
            }
        }
        Text(
            text = stringResource(Res.string.feature_auth_change_tenant),
            style = MifosTypography.labelMedium,
            color = KptTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onChangeClick),
        )
    }
}

@Preview
@Composable
private fun TenantHeaderCardPreview() {
    MifosMobileTheme {
        TenantHeaderCard(
            tenantName = "Kampala Teachers SACCO",
            onChangeClick = {},
            modifier = Modifier.padding(KptTheme.spacing.md),
        )
    }
}
