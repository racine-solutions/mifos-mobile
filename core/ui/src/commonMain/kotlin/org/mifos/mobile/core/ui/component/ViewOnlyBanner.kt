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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import mifos_mobile.core.ui.generated.resources.Res
import mifos_mobile.core.ui.generated.resources.feature_view_only_banner
import mifos_mobile.core.ui.generated.resources.feature_view_only_call
import org.jetbrains.compose.resources.stringResource
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.ui.utils.ShareUtils
import template.core.base.designsystem.theme.KptTheme

private val BannerBackground = Color(0xFFFAEEDA)
private val BannerText = Color(0xFF633806)

/** Fixed notice shown on every screen while the user is view-only, with one call button per support number. */
@Composable
fun ViewOnlyBanner(
    phoneNumbers: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BannerBackground)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.feature_view_only_banner),
            style = MifosTypography.labelMedium,
            color = BannerText,
        )
        phoneNumbers.forEach { number ->
            TextButton(onClick = { ShareUtils.openUrl("tel:${number.filter { it.isDigit() || it == '+' }}") }) {
                Text(
                    text = stringResource(Res.string.feature_view_only_call, number),
                    style = MifosTypography.labelMedium,
                    color = BannerText,
                )
            }
        }
    }
}
