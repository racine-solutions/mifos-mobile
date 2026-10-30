

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.mifos.mobile.libs

class AndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // google-services.json is project specific and is not kept in the repository. Only
            // apply the Firebase Gradle plugins when the project provides its own config file.
            val hasGoogleServicesConfig = file("google-services.json").exists()
            if (hasGoogleServicesConfig) {
                apply(plugin = "com.google.gms.google-services")
                apply(plugin = "com.google.firebase.crashlytics")
            }

            dependencies {
                val bom = libs.findLibrary("firebase-bom").get()
                "implementation"(platform(bom))
                "implementation"(libs.findLibrary("firebase.analytics").get())
                "implementation"(libs.findLibrary("firebase.crashlytics").get())
            }

            if (!hasGoogleServicesConfig) return

            extensions.configure<ApplicationExtension> {
                buildTypes.configureEach {
                    // Disable the Crashlytics mapping file upload. This feature should only be
                    // enabled if a Firebase backend is available and configured in
                    // google-services.json.
                    configure<CrashlyticsExtension> {
                        mappingFileUploadEnabled = false
                    }
                }
            }
        }
    }
}
