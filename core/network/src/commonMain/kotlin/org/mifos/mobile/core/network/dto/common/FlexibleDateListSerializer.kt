/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.mifos.mobile.core.network.dto.common

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Fineract usually represents a date as a `[year, month, day]` array, but some endpoints
 * (observed on the same server instance, e.g. self/savingsaccounts/{id}) instead return a
 * plain "yyyy-MM-dd" string for the same field. This normalizes either shape into the
 * `List<Int>` form the rest of the codebase already expects, so a DTO field only needs
 * `@Serializable(with = FlexibleDateListSerializer::class)` to tolerate both.
 */
object FlexibleDateListSerializer : JsonTransformingSerializer<List<Int>>(ListSerializer(Int.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonPrimitive || !element.isString) return element
        val parts = element.content.split("-").mapNotNull { it.toIntOrNull() }
        return JsonArray(parts.map { JsonPrimitive(it) })
    }
}
