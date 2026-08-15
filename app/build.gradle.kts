import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Release signing credentials.
 *
 * Read from `keystore.properties` (gitignored, sits next to this repo but is
 * never part of it) and falling back to environment variables so CI can sign
 * from secrets. If neither is present the release build is simply left
 * unsigned rather than failing — a contributor cloning this repo should still
 * be able to run `assembleRelease`.
 *
 * The keystore file itself lives outside the working tree entirely.
 */
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "RANGE_STORE_FILE")
val hasSigningConfig = releaseStoreFile != null && file(releaseStoreFile).exists()

/**
 * Live-fare API credentials.
 *
 * Same shape as the signing config above — `local.properties` (gitignored)
 * with an environment-variable fallback so CI can inject from secrets.
 *
 * A missing key is **not** an error. The provider reports itself unconfigured,
 * the repository skips it, and every price falls back to the model — which is
 * exactly what a fresh clone of a public repo should do.
 *
 * Note these are compiled into BuildConfig and therefore ship inside the APK,
 * where a determined person can read them back out. That is an acceptable
 * trade for the free, rate-limited tiers used here: the worst case is someone
 * burning a public quota. Never put a credential with a bill attached here —
 * anything metered belongs behind a proxy you control.
 */
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun credential(property: String, env: String): String =
    (localProperties.getProperty(property) ?: System.getenv(env) ?: "").trim()

android {
    namespace = "com.vythera.range"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vythera.range"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AMADEUS_CLIENT_ID", "\"${credential("amadeus.clientId", "AMADEUS_CLIENT_ID")}\"")
        buildConfigField("String", "AMADEUS_CLIENT_SECRET", "\"${credential("amadeus.clientSecret", "AMADEUS_CLIENT_SECRET")}\"")
        // Amadeus ships a free test environment and a paid production one on
        // identical paths. Default to test — production needs a card on file,
        // so silently defaulting to it would be a nasty surprise.
        buildConfigField(
            "String",
            "AMADEUS_HOST",
            "\"${credential("amadeus.host", "AMADEUS_HOST").ifEmpty { "test.api.amadeus.com" }}\"",
        )
        buildConfigField("String", "TRAVELPAYOUTS_TOKEN", "\"${credential("travelpayouts.token", "TRAVELPAYOUTS_TOKEN")}\"")
        buildConfigField("String", "TRAVELPAYOUTS_MARKER", "\"${credential("travelpayouts.marker", "TRAVELPAYOUTS_MARKER")}\"")
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "RANGE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RANGE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RANGE_KEY_PASSWORD")
                // Both signature schemes: v1 for API 26-27, v2/v3 for modern
                // installs and faster verification.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // So the Settings screen can show the version it was actually built
        // with instead of a literal that drifts.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            // The fare providers log through android.util.Log, which is a stub
            // that throws under plain JUnit. Without this, testing an error
            // path means testing the logging framework instead.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
