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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import mifos_mobile.feature.auth.generated.resources.Res
import mifos_mobile.feature.auth.generated.resources.feature_auth_call_number
import mifos_mobile.feature.auth.generated.resources.feature_auth_ok
import mifos_mobile.feature.auth.generated.resources.feature_blocked_copy_reference
import mifos_mobile.feature.auth.generated.resources.feature_blocked_title
import org.jetbrains.compose.resources.stringResource
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.model.ApiError
import org.mifos.mobile.core.model.ApiErrorCodes
import org.mifos.mobile.core.ui.utils.ShareUtils
import template.core.base.designsystem.theme.KptTheme

/**
 * The member's phone or email is also on another record, so the server blocks the app until the
 * SACCO corrects the records. Quote [reference] to staff; there is deliberately no retry.
 */
data class ContactBlock(
    val message: String,
    val reference: String,
    val phoneNumbers: List<String>,
)

/** Returns the block to show when [error] is the shared-contact answer, otherwise null. */
internal fun ApiError?.asContactBlock(): ContactBlock? =
    if (this?.code == ApiErrorCodes.CONTACT_CONFLICT) {
        ContactBlock(userMessage, reference.orEmpty(), supportPhoneNumbers)
    } else {
        null
    }

@Composable
internal fun ContactConflictDialog(
    block: ContactBlock,
    onDismissRequest: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(Res.string.feature_blocked_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm)) {
                Text(
                    text = block.message,
                    style = MifosTypography.bodyMedium,
                    color = KptTheme.colorScheme.onSurface,
                )
                if (block.reference.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = block.reference,
                            style = MifosTypography.headlineSmall,
                            color = KptTheme.colorScheme.primary,
                        )
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(block.reference)) }) {
                        Text(text = stringResource(Res.string.feature_blocked_copy_reference))
                    }
                }
                block.phoneNumbers.forEach { number ->
                    TextButton(onClick = { ShareUtils.openUrl("tel:${number.filter { it.isDigit() || it == '+' }}") }) {
                        Text(text = stringResource(Res.string.feature_auth_call_number, number))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(text = stringResource(Res.string.feature_auth_ok)) }
        },
    )
}
