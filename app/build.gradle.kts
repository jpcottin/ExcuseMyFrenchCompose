plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.10"
}



android {
    namespace = "com.example.excusemyfrenchcompose"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.excusemyfrenchcompose"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(libs.okhttp)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Unit Tests
    testImplementation("junit:junit:4.13.2") // Or the latest version
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For testing coroutines
    testImplementation("androidx.arch.core:core-testing:2.2.0") // For testing LiveData/StateFlow
    testImplementation("io.mockk:mockk:1.13.9") // Mocking library (optional but very useful)

    // UI Tests (Compose)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1") // Use the correct version for your Compose version
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5") // Kotlin extensions for JUnit
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso (optional, for interacting with non-Compose views)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}