/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
@file:Suppress("MatchingDeclarationName")

package org.mifos.mobile.feature.auth.verifyCode

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import org.mifos.mobile.core.ui.composableWithStayTransitions

@Serializable
data object VerifyCodeRoute

fun NavController.navigateToVerifyCodeScreen(navOptions: NavOptions? = null) {
    this.navigate(VerifyCodeRoute, navOptions)
}

fun NavGraphBuilder.verifyCodeDestination(
    navigateToLogin: () -> Unit,
    navigateBackToSignup: (String?) -> Unit,
    navigateBack: () -> Unit,
) {
    composableWithStayTransitions<VerifyCodeRoute> {
        VerifyCodeScreen(
            navigateToLogin = navigateToLogin,
            navigateBackToSignup = navigateBackToSignup,
            navigateBack = navigateBack,
        )
    }
}
