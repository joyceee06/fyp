import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.compose)
    // Register KSP to make Room work
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.fyp.ekopantri"
    compileSdk = 36 // Note: SDK 36 is Preview/Very new, ensure your Android Studio is updated

    defaultConfig {
        applicationId = "com.fyp.ekopantri"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = Properties()
        val propFile = rootProject.file("local.properties")
        if (propFile.exists()) {
            propFile.inputStream().use { props.load(it) }
        }

        val scannerKey = props.getProperty("GEMINI_SCANNER_KEY") ?: ""
        val educationKey = props.getProperty("GEMINI_EDUCATION_KEY") ?: ""
        val recipeKey = props.getProperty("SPOONACULAR_KEY") ?: ""

        buildConfigField("String", "GEMINI_SCANNER_KEY", "\"$scannerKey\"")
        buildConfigField("String", "GEMINI_EDUCATION_KEY", "\"$educationKey\"")
        buildConfigField("String", "SPOONACULAR_KEY", "\"$recipeKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation(libs.firebase.perf)
    implementation(libs.google.generativeai)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.database)

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-storage")

    // Navigation & ML Kit
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Networking (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Database (Room)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // KSP handles the code generation for Room
    ksp("androidx.room:room-compiler:$roomVersion")
}
