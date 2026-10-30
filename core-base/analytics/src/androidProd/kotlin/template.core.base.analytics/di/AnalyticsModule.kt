/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
@file:Suppress("InvalidPackageDeclaration")

package template.core.base.analytics.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import template.core.base.analytics.AnalyticsHelper
import template.core.base.analytics.StubAnalyticsHelper

// Firebase is intentionally not wired in until this app has its own Firebase project
// (google-services.json for its application id). Until then no analytics are collected.
actual val analyticsModule: Module = module {
    singleOf(::StubAnalyticsHelper) bind AnalyticsHelper::class
}
