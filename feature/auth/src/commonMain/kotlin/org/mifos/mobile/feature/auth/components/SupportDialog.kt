/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.feature.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_auth_call_number
import org.jetbrains.compose.resources.stringResource
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.ui.utils.ShareUtils
import template.core.base.designsystem.theme.KptTheme

/** A secondary action shown next to the main button of a [SupportDialog]. */
internal data class DialogAction(val label: String, val onClick: () -> Unit)

/**
 * The bank's explanation of what happened, with one tap-to-call button per support number the
 * server sent. [extraAction] adds a second button (for example "Reset my password").
 */
@Composable
internal fun SupportDialog(
    message: String,
    phoneNumbers: List<String>,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    extraAction: DialogAction? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
                Text(
                    text = message,
                    style = MifosTypography.bodyMedium,
                    color = KptTheme.colorScheme.onSurface,
                )
                phoneNumbers.forEach { number ->
                    TextButton(onClick = { ShareUtils.openUrl("tel:${number.filter { it.isDigit() || it == '+' }}") }) {
                        Text(text = stringResource(Res.string.feature_auth_call_number, number))
                    }
                }
            }
        },
        dismissButton = extraAction?.let { action ->
            { TextButton(onClick = action.onClick) { Text(text = action.label) } }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = confirmLabel) }
        },
    )
}
