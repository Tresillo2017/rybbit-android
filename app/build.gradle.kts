plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.rybbitclient"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.rybbitclient"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Debug-signed so `assembleRelease` produces an installable APK out of the box.
            // Replace with a real signing config before distributing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // Robolectric + Roborazzi render real Android resources (strings, themes, fonts).
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // ComponentActivity for createAndroidComposeRule in JVM tests (manifest entry + test APIs).
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}

// Screenshot tests render 16 real screens and write PNGs, so they run only for Roborazzi tasks
// (recordRoborazziDebug / verifyRoborazziDebug), not in the default testDebugUnitTest run.
val isRoborazziRun = gradle.startParameter.taskNames.any { it.contains("Roborazzi", ignoreCase = true) }
tasks.withType<Test>().configureEach {
    if (!isRoborazziRun) exclude("app/rybbitclient/screenshots/**")
}

// Robolectric runs Compose's idling strategy through Espresso when it is on the classpath. Espresso 3.5.x
// fails its input-injection reflection on SDK 37 (NoSuchMethodException), so the unit-test classpath
// pins the newer 3.7.0 release that Compose's ui-test pulls in transitively.
configurations.matching { it.name.contains("UnitTest") }.configureEach {
    resolutionStrategy.force("androidx.test.espresso:espresso-core:3.7.0")
}
