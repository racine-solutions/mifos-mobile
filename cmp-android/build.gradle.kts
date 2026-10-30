/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
import org.mifos.mobile.MifosBuildType
import org.mifos.mobile.dynamicVersion

plugins {
    alias(libs.plugins.mifos.android.application)
    alias(libs.plugins.mifos.android.application.compose)
    alias(libs.plugins.mifos.android.application.flavors)
    id("com.google.android.gms.oss-licenses-plugin")
    id("com.google.devtools.ksp")
}

val packageNameSpace: String = libs.versions.androidPackageNamespace.get()

android {
    namespace = "cmp.android.app"

    defaultConfig {
        applicationId = packageNameSpace
        versionName = System.getenv("VERSION") ?: project.dynamicVersion
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing material is never stored in the repository. Provide it through the
    // environment (CI) or ~/.gradle/gradle.properties (local), see docs/play-store-release.md.
    val releaseKeystorePath: String? =
        System.getenv("KEYSTORE_PATH") ?: providers.gradleProperty("RELEASE_KEYSTORE_PATH").orNull
    val hasReleaseSigning = releaseKeystorePath != null && file(releaseKeystorePath).exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: providers.gradleProperty("RELEASE_KEYSTORE_PASSWORD").orNull
                keyAlias = System.getenv("KEYSTORE_ALIAS")
                    ?: providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
                keyPassword = System.getenv("KEYSTORE_ALIAS_PASSWORD")
                    ?: providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = MifosBuildType.DEBUG.applicationIdSuffix
        }

        // Disabling proguard for now until
        // https://github.com/openMF/mobile-wallet/issues/1815 this issue is resolved
        release {
            isMinifyEnabled = false
            applicationIdSuffix = MifosBuildType.RELEASE.applicationIdSuffix
            isShrinkResources = false
            isDebuggable = false
            isJniDebuggable = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        xmlReport = true
        checkDependencies = true
        abortOnError = false
        // Disable this rule until we ship the libraries to some maven.
        disable += "ResourceName"
        disable += "MissingTranslation"
        disable += "ExtraTranslation"
        baseline = File("lint-baseline.xml")
        explainIssues = true
        htmlReport = true
    }
}
dependencyGuard {
    configuration("demoDebugRuntimeClasspath")
    configuration("demoReleaseRuntimeClasspath")
    configuration("prodDebugRuntimeClasspath")
    configuration("prodReleaseRuntimeClasspath")
}

dependencies {
    implementation(projects.cmpShared)
    implementation(projects.core.ui)
    implementation(projects.coreBase.ui)
    implementation(projects.coreBase.analytics)

    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.datastore)

    implementation(projects.coreBase.platform)

    // Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing.ktx)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    runtimeOnly(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test)

    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.test.ext.junit)

    implementation(libs.filekit.core)
    implementation(libs.filekit.compose)
    implementation(libs.filekit.dialog.compose)
    implementation(libs.google.oss.licenses)

    testImplementation(kotlin("test"))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath") {
        modules = true
        tree = true
    }
}

// OSS licenses plugin resolves a compile classpath variant that is ambiguous with cmp-shared KMP outputs.
// Disable release-only OSS aggregation tasks so full `build` remains green while app debug builds keep working.
tasks.configureEach {
    if (name.endsWith("ReleaseOssLicensesTask")) {
        enabled = false
    }
}
