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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import template.core.base.designsystem.theme.KptTheme

/**
 * Square badge showing the initials of the organisation (Sacco/MFI) the user belongs to.
 * Used until a tenant logo is available from the backend.
 */
@Composable
fun TenantBadge(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MifosTypography.titleMediumEmphasized,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(KptTheme.shapes.medium)
            .background(KptTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.tenantInitials(),
            style = textStyle,
            color = KptTheme.colorScheme.onPrimary,
        )
    }
}

/** First letters of the first two words, e.g. "Kampala Teachers SACCO" becomes "KT". */
fun String.tenantInitials(): String {
    val words = trim().split(" ", "-", "_").filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}
