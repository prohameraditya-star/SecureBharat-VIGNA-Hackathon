plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Use the alias from libs.versions.toml for consistency
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.paisacheck360"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.paisacheck360"
        minSdk = 24
        targetSdk = 36
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
        viewBinding = true
        mlModelBinding = true
    }
}

dependencies {
    // ✅ FIX: Add a direct dependency to resolve the version conflict
    implementation("androidx.annotation:annotation-experimental:1.4.0")

    // ✅ Core AndroidX libs
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // ✅ OCR / QR Scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // QR Scanner
    implementation("com.google.mlkit:text-recognition:16.0.0")     // OCR

    // ✅ TensorFlow Lite for phishing detection model
    implementation("org.tensorflow:tensorflow-lite:2.11.0")

    // ✅ Networking (Duplicates removed)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ Coroutines – async tasks (Duplicate removed)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ✅ Firebase (BoM controls versions, duplicates removed)
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ✅ Glide (image loading)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // ✅ YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // ✅ Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}