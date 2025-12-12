plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.example.rsrtest"
    compileSdk = 36
    packaging {
        jniLibs {
            pickFirsts += setOf("**/*.so", "META-INF/LICENSE*", "META-INF/NOTICE*")
        }
        resources {
            pickFirsts += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
        }
    }

    // Evitar conflictos al empaquetar las .so de PyTorch

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=androidx.camera.core.ExperimentalGetImage"
        )
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // -------------------------------------
    // 1) Core de Android + Jetpack Compose
    // -------------------------------------
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Compose BOM (gestiona versiones)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime:1.4.8")

    // Concurrency + Guava
    implementation("androidx.concurrent:concurrent-futures:1.3.0")
    implementation("com.google.guava:guava:33.4.8-android")

    // -------------------------------------
    // 2) CameraX
    // -------------------------------------
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // -------------------------------------
    // 3) ML Kit Object Detection
    // -------------------------------------
    implementation("com.google.mlkit:object-detection:17.0.2")

    // -------------------------------------
    // 4) Play Services Location
    // -------------------------------------
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // -------------------------------------
    // 5) Accompanist Permissions
    // -------------------------------------
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    
    // -------------------------------------
    // 5b) Coil for image loading
    // -------------------------------------
    implementation("io.coil-kt:coil-compose:2.7.0")

    // -------------------------------------
    // 6) Firebase BoM + Firestore + Analytics
    // -------------------------------------
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // -------------------------------------
    // 7) TensorFlow Lite para YOLO (sin PyTorch para evitar conflictos)
    // -------------------------------------
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")

    // -------------------------------------
    // 8) Room Database
    // -------------------------------------
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // -------------------------------------
    // 9) Dependency Injection (Manual temporalmente)
    // -------------------------------------
    // Hilt temporalmente deshabilitado por compatibilidad

    // -------------------------------------
    // 10) Testing & Debug
    // -------------------------------------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
