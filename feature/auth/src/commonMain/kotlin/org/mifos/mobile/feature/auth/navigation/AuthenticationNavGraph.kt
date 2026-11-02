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

package org.mifos.mobile.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.navigation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mifos.mobile.feature.auth.login.LoginRoute
import org.mifos.mobile.feature.auth.login.loginDestination
import org.mifos.mobile.feature.auth.login.navigateToLoginScreen
import org.mifos.mobile.feature.auth.otpAuthentication.navigateToOtpAuthScreen
import org.mifos.mobile.feature.auth.otpAuthentication.otpAuthenticationDestination
import org.mifos.mobile.feature.auth.recoverPassword.navigateToRecoverPasswordScreen
import org.mifos.mobile.feature.auth.recoverPassword.recoverPasswordDestination
import org.mifos.mobile.feature.auth.registration.navigateToRegisterScreen
import org.mifos.mobile.feature.auth.registration.RegistrationRoute
import org.mifos.mobile.feature.auth.registration.registrationDestination
import org.mifos.mobile.feature.auth.resetPassword.navigateToResetPasswordScreen
import org.mifos.mobile.feature.auth.resetPassword.resetPasswordDestination
import org.mifos.mobile.feature.auth.setNewPassword.navigateToSetPasswordScreen
import org.mifos.mobile.feature.auth.setNewPassword.setPasswordDestination
import org.mifos.mobile.feature.auth.uploadId.uploadIdDestination
import org.mifos.mobile.feature.auth.verifyCode.navigateToVerifyCodeScreen
import org.mifos.mobile.feature.auth.verifyCode.verifyCodeDestination

@Serializable
@SerialName("auth_graph")
data object AuthGraphRoute

fun NavController.navigateToAuthGraph(navOptions: NavOptions? = null) {
    this.navigate(AuthGraphRoute, navOptions)
}

fun NavGraphBuilder.authenticationNavGraph(
    navController: NavHostController,
    navigateToPasscodeScreen: () -> Unit,
    navigateToStatusScreen: (String, String, String, String, String) -> Unit,
) {
    navigation<AuthGraphRoute>(
        startDestination = LoginRoute,
    ) {
        loginDestination(
            navigateToRegisterScreen = navController::navigateToRegisterScreen,
            navigateToPasscodeScreen = navigateToPasscodeScreen,
            navigateToForgotPasswordScreen = navController::navigateToRecoverPasswordScreen,
            navigateToConfirmCodeScreen = { navController.navigateToOtpAuthScreen() },
            navigateToSetPasswordScreen = { navController.navigateToSetPasswordScreen() },
        )

        registrationDestination(
            navigateToLoginScreen = navController::navigateToLoginScreen,
            navigateToVerifyCodeScreen = navController::navigateToVerifyCodeScreen,
            navigateToForgotPasswordScreen = navController::navigateToRecoverPasswordScreen,
        )

        verifyCodeDestination(
            navigateToLogin = navController::navigateToLoginScreen,
            navigateBackToSignup = { navController.popBackStack(RegistrationRoute, inclusive = false) },
            navigateBack = navController::popBackStack,
        )

        resetPasswordDestination(
            navigateToLogin = navController::navigateToLoginScreen,
            navigateBack = navController::popBackStack,
        )

        uploadIdDestination(
            navigateToRegisterScreen = navController::navigateToRegisterScreen,
            navigateToOtpAuthenticationScreen = navController::navigateToOtpAuthScreen,
        )

        otpAuthenticationDestination(
            navigateBack = navController::popBackStack,
            navigateToStatusScreen = navigateToStatusScreen,
            navigateToSetPasswordScreen = navController::navigateToSetPasswordScreen,
        )

        recoverPasswordDestination(
            navigateToLoginScreen = navController::navigateToLoginScreen,
            navigateToResetPasswordScreen = navController::navigateToResetPasswordScreen,
        )

        setPasswordDestination(
            navigateToStatusScreen = navigateToStatusScreen,
            navigateToLoginScreen = navController::navigateToLoginScreen,
        )
    }
}
