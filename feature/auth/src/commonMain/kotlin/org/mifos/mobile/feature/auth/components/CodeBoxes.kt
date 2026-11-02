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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import org.mifos.mobile.core.model.access.CODE_LENGTH
import template.core.base.designsystem.theme.KptTheme

/**
 * One-time code input drawn as separate boxes. A single hidden text field receives the typing, so
 * paste, the keyboard and autofill all work as for any normal field.
 */
@Composable
internal fun CodeBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    length: Int = CODE_LENGTH,
    onDone: () -> Unit = {},
) {
    BasicTextField(
        value = code,
        onValueChange = { typed -> onCodeChange(typed.filter(Char::isDigit).take(length)) },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.size(0.dp)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(length) { index ->
                        val filled = index < code.length
                        val borderColor = when {
                            isError -> KptTheme.colorScheme.error
                            index == code.length && enabled -> KptTheme.colorScheme.primary
                            else -> KptTheme.colorScheme.outlineVariant
                        }
                        Surface(
                            modifier = Modifier.size(width = 46.dp, height = 56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (enabled) KptTheme.colorScheme.surface else KptTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (filled || index == code.length) 2.dp else 1.dp, borderColor),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (filled) code[index].toString() else "",
                                    style = MifosTypography.headlineSmall,
                                    color = KptTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
