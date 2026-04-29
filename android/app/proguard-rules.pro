# GoThere ProGuard Rules

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Play Billing
-keep class com.android.vending.billing.** { *; }

# Keep Firestore model classes (used with Firestore document mapping)
-keep class com.example.gothere.model.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose
-dontwarn androidx.compose.**

# Keep R8 from stripping data classes used in serialization
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}
