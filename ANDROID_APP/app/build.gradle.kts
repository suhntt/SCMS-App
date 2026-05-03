plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // ✅ THIS WAS MISSING
    id("com.google.gms.google-services")
    alias(libs.plugins.ksp)
}

android {

    namespace = "com.example.scms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.scms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // 🚀 DEBUG: Optimized for FAST builds during development
        debug {
            isMinifyEnabled = false          // No code shrinking
            isShrinkResources = false        // No resource shrinking  
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 🚀 Only package English locale resources (saves packaging time)
    androidResources {
        localeFilters += listOf("en")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/*.txt"
            excludes += "/okio/**"
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.activity:activity:1.9.3")
            force("androidx.activity:activity-compose:1.9.3")
            force("androidx.activity:activity-ktx:1.9.3")
        }
    }
}

dependencies {

    /* -----------------------------
       CORE AND COMPOSE
    ------------------------------ */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation(libs.androidx.activity.compose)
    // 🔥 Firebase (CORRECT – via BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging") // NEW: Push Notifications
    implementation("com.google.guava:guava:33.0.0-android")



    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    /* -----------------------------
       NAVIGATION
    ------------------------------ */
    implementation("androidx.navigation:navigation-compose:2.7.7")

    /* -----------------------------
       CAMERA X (ONLY ONCE)
    ------------------------------ */
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("androidx.compose.material:material-icons-extended")





    /* -----------------------------
       LOCATION
    ------------------------------ */
    implementation(
        "com.google.android.gms:play-services-location:21.0.1"
    )
    implementation("com.google.android.gms:play-services-nearby:18.7.0") // 📶 MESH NETWORKING

    /* -----------------------------
       NETWORKING
    ------------------------------ */
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(
        "com.squareup.retrofit2:converter-gson:2.9.0"
    )
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    /* -----------------------------
       IMAGE LOADING
    ------------------------------ */
    implementation("io.coil-kt:coil-compose:2.6.0")

    /* -----------------------------
       MAP (OSMDROID – NATIVE)
    ------------------------------ */
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    /* -----------------------------
       TESTING
    ------------------------------ */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    /* -----------------------------
       WORK MANAGER
    ------------------------------ */
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    /* -----------------------------
       ROOM DATABASE
    ------------------------------ */
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
