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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mifos_mobile.feature.home.generated.resources.Res
import mifos_mobile.feature.home.generated.resources.feature_home_accounts_count
import mifos_mobile.feature.home.generated.resources.feature_home_accounts_total
import mifos_mobile.feature.home.generated.resources.feature_home_filter_all
import mifos_mobile.feature.home.generated.resources.feature_home_filter_loans
import mifos_mobile.feature.home.generated.resources.feature_home_filter_savings
import mifos_mobile.feature.home.generated.resources.feature_home_filter_shares
import mifos_mobile.feature.home.generated.resources.feature_home_my_accounts
import mifos_mobile.feature.home.generated.resources.feature_home_status_active
import mifos_mobile.feature.home.generated.resources.feature_home_status_closed
import mifos_mobile.feature.home.generated.resources.feature_home_status_pending
import org.jetbrains.compose.resources.stringResource
import org.mifos.mobile.core.common.Constants
import org.mifos.mobile.core.designsystem.icon.MifosIcons
import org.mifos.mobile.core.designsystem.theme.MifosTypography
import template.core.base.designsystem.theme.KptTheme

/**
 * One wallet card: the total for an entity (savings, loans or shares) and how many of its
 * accounts are active, closed or pending.
 */
@Immutable
internal data class WalletEntity(
    val type: String,
    val count: Int,
    val total: String,
    val active: Int,
    val closed: Int,
    val pending: Int,
)

private val InkCard = Color(0xFF161A22)
private val BlueCard = Color(0xFF2874EC)
private val PurpleCard = Color(0xFF7C3AED)
private val MintAccent = Color(0xFF60F1A0)

/**
 * One card per entity with the totals, plus filter chips showing how many accounts of each kind
 * the member has. Uses only the accounts the Home screen already loads.
 */
@Composable
internal fun AccountWallet(
    entities: List<WalletEntity>,
    isAmountVisible: Boolean,
    onToggleAmountVisible: () -> Unit,
    onEntityClick: (type: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filters = buildList {
        add(WalletFilterChip(null, stringResource(Res.string.feature_home_filter_all)))
        entities.forEach { entity ->
            val name = when (entity.type) {
                Constants.SAVINGS_ACCOUNT -> stringResource(Res.string.feature_home_filter_savings)
                Constants.LOAN_ACCOUNT -> stringResource(Res.string.feature_home_filter_loans)
                else -> stringResource(Res.string.feature_home_filter_shares)
            }
            add(WalletFilterChip(entity.type, "$name ${entity.count}"))
        }
    }
    val selected = filters.getOrElse(filterIndex) { filters.first() }
    val visibleEntities = remember(entities, selected.type) {
        entities.filter { selected.type == null || it.type == selected.type }
    }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.feature_home_my_accounts),
                style = MifosTypography.titleMediumEmphasized,
                color = KptTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.feature_home_accounts_total, entities.sumOf { it.count }),
                    style = MifosTypography.labelMedium,
                    color = KptTheme.colorScheme.primary,
                )
                IconButton(onClick = onToggleAmountVisible, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isAmountVisible) MifosIcons.Eye else MifosIcons.EyeOff,
                        contentDescription = null,
                        tint = KptTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(visibleEntities, key = { it.type }) { entity ->
                WalletCard(
                    entity = entity,
                    isAmountVisible = isAmountVisible,
                    onClick = { onEntityClick(entity.type) },
                    modifier = if (visibleEntities.size == 1) Modifier.fillParentMaxWidth() else Modifier,
                )
            }
        }

        if (visibleEntities.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KptTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val current = listState.firstVisibleItemIndex.coerceIn(0, visibleEntities.lastIndex)
                visibleEntities.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == current) 16.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == current) {
                                    KptTheme.colorScheme.onSurface
                                } else {
                                    KptTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = KptTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            filters.forEachIndexed { index, chip ->
                val isSelected = index == filterIndex
                Text(
                    text = chip.label,
                    style = MifosTypography.labelMedium,
                    color = if (isSelected) KptTheme.colorScheme.onPrimary else KptTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                KptTheme.colorScheme.onSurface
                            } else {
                                KptTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .clickable(role = Role.Tab) { filterIndex = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private data class WalletFilterChip(val type: String?, val label: String)

@Composable
private fun WalletCard(
    entity: WalletEntity,
    isAmountVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (background, circle, title) = when (entity.type) {
        Constants.LOAN_ACCOUNT -> Triple(
            BlueCard,
            Color.White.copy(alpha = 0.2f),
            stringResource(Res.string.feature_home_filter_loans),
        )
        Constants.SHARE_ACCOUNTS -> Triple(
            PurpleCard,
            Color.White.copy(alpha = 0.2f),
            stringResource(Res.string.feature_home_filter_shares),
        )
        else -> Triple(
            InkCard,
            MintAccent,
            stringResource(Res.string.feature_home_filter_savings),
        )
    }
    Box(
        modifier = modifier
            .width(264.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(circle),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MifosTypography.titleSmallEmphasized,
                color = Color.White,
            )
            Text(
                text = stringResource(Res.string.feature_home_accounts_count, entity.count),
                style = MifosTypography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isAmountVisible) entity.total else "••••••••",
                style = MifosTypography.titleLargeEmphasized,
                color = Color.White,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(stringResource(Res.string.feature_home_status_active, entity.active), Color(0xFF60F1A0))
                StatusPill(stringResource(Res.string.feature_home_status_closed, entity.closed), Color.White)
                if (entity.pending > 0) {
                    StatusPill(stringResource(Res.string.feature_home_status_pending, entity.pending), Color(0xFFFFD36B))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, tint: Color) {
    Text(
        text = text,
        style = MifosTypography.labelSmall,
        color = tint,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
