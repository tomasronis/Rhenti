plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.tomasronis.rhentiapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.com.rhentiMobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields for API endpoints
        buildConfigField("String", "API_BASE_URL_PROD", "\"https://api.rhenti.com\"")
        buildConfigField("String", "API_BASE_URL_UAT", "\"https://uatapi.rhenti.com\"")
        buildConfigField("String", "API_BASE_URL_DEMO", "\"https://demo.rhenti.com\"")
        buildConfigField("String", "API_IMAGE_URL_PROD", "\"https://upploader.rhenti.com/images/\"")
        buildConfigField("String", "API_IMAGE_URL_UAT", "\"https://uatimgs.rhenti.com/images/\"")

        // White Label Configuration
        buildConfigField("String", "WHITE_LABEL", "\"rhenti_mobile\"")

        // Google OAuth Configuration (placeholder - user must replace with actual client ID)
        manifestPlaceholders["googleClientId"] = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"YOUR_WEB_CLIENT_ID\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            isMinifyEnabled = false
        }
        release {
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore & Security
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Twilio Voice (Phase 7)
    implementation(libs.twilio.voice)

    // Google Auth
    implementation(libs.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.credentials.play.services)
    implementation(libs.credentials)

    // Microsoft Auth
    implementation(libs.microsoft.authenticator)

    // Firebase (will be added in Phase 8)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.messaging)
    // implementation(libs.firebase.analytics)

    // WorkManager (will be added in Phase 9)
    // implementation(libs.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}