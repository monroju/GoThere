import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val localProps = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

android {
    namespace = "com.example.gothere"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res")
        }
    }

    defaultConfig {
        applicationId = "com.gothere.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 32
        versionName = "1.11.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("RELEASE_STORE_FILE", "../gothere-release.keystore"))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            // keep default
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        // Uncomment during hardening if needed:
        // freeCompilerArgs += listOf("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
    }

    packaging {
        resources {
            // avoid common META-INF collisions from transitive deps
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 16 KB native page size alignment (Play Console enforcement, Nov 2025)
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ---------------- Compose BOM keeps UI libs in sync ----------------
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Core + Lifecycle + Activity
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.3")

    // MapLibre GL for city maps — 11.8+ ships 16 KB-aligned native libs
    implementation("org.maplibre.gl:android-sdk:11.13.0")

    // In-App Review
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Compose UI (versions come from BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Datastore (you referenced it)
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase (use BOM to align) — 33.x ships 16 KB-aligned native libs
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Play Billing — 8.x required by Play (Aug 31 2026 deadline); ships 16 KB-aligned native libs
    implementation("com.android.billingclient:billing-ktx:8.3.0")

    // PostHog product analytics
    implementation("com.posthog:posthog-android:3.+")

    // (Optional) Material Components for Android Views (if any XML screens)
    implementation("com.google.android.material:material:1.12.0")

    // ---------- Tests (lightweight defaults) ----------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
