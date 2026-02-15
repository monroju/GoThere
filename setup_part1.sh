set -e

echo "==> Working dir: $PWD"
mkdir -p android/app/src/main/java/com/example/gothere/{ui,ui/theme,model,repository,viewmodel}
mkdir -p android/app/src/main/res/{values,xml,drawable}

# settings & top-level gradle
cat > android/settings.gradle.kts <<'KOT'
rootProject.name = "GoThere"
include(":app")
KOT

cat > android/build.gradle.kts <<'KOT'
plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
KOT

# app module gradle
cat > android/app/build.gradle.kts <<'KOT'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.gothere"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.gothere"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
}
KOT

# manifest
cat > android/app/src/main/AndroidManifest.xml <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.gothere">
  <application android:allowBackup="true" android:label="GoThere" android:theme="@style/Theme.GoThere">
    <activity android:name=".MainActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>
  </application>
</manifest>
XML

# theme placeholder (needed by manifest)
mkdir -p android/app/src/main/res/values
cat > android/app/src/main/res/values/themes.xml <<'XML'
<resources>
  <style name="Theme.GoThere" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
XML

# minimal Compose activity so you see “GoThere skeleton ✅”
cat > android/app/src/main/java/com/example/gothere/MainActivity.kt <<'KOT'
package com.example.gothere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme { Text("GoThere skeleton ✅") } }
  }
}
KOT

echo "==> Created files:"
ls -R android | sed -n '1,160p'
