plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.example.rsrtest"
    compileSdk = 35

    // Evitar conflictos al empaquetar las .so de PyTorch
    packagingOptions {
        pickFirst("**/*.so")
        pickFirst("META-INF/LICENSE*")
        pickFirst("META-INF/NOTICE*")
    }

    defaultConfig {
        applicationId = "com.example.rsrtest"
        minSdk = 33
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
        freeCompilerArgs += listOf(
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=androidx.camera.core.ExperimentalGetImage"
        )
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // -------------------------------------
    // 1) Core de Android + Jetpack Compose
    // -------------------------------------
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM (gestiona versiones)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime:1.5.8")

    // Concurrency + Guava
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("com.google.guava:guava:31.1-android")

    // -------------------------------------
    // 2) CameraX
    // -------------------------------------
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // -------------------------------------
    // 3) ML Kit Object Detection
    // -------------------------------------
    implementation("com.google.mlkit:object-detection:17.0.1")

    // -------------------------------------
    // 4) Play Services Location
    // -------------------------------------
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // -------------------------------------
    // 5) Accompanist Permissions
    // -------------------------------------
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // -------------------------------------
    // 6) Firebase BoM + Firestore + Analytics
    // -------------------------------------
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // -------------------------------------
    // 7) PyTorch Mobile
    // -------------------------------------
    implementation("org.pytorch:pytorch_android:1.13.0")
    implementation("org.pytorch:pytorch_android_torchvision:1.13.0")

    // -------------------------------------
    // 8) Room Database
    // -------------------------------------
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // -------------------------------------
    // 9) Testing & Debug
    // -------------------------------------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
