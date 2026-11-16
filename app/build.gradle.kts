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
        versionCode = 3
        versionName = "0.2.1"
    }

    // A fixed release key keeps updates installable: Android only updates an app in place when the new APK is signed
    // with the same certificate. CI provides these from repository secrets; without them (local builds) the release
    // build falls back to the debug key so `assembleRelease` still works, but that APK cannot update a release install.
    val releaseKeystore = providers.environmentVariable("RYBBIT_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("RYBBIT_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RYBBIT_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RYBBIT_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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

kotlin {
    compilerOptions {
        // Expressive components are opt-in APIs; enabling once here keeps screens free of per-file annotations.
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
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
