plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.clockstopper.deepparallax"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.clockstopper.deepparallax"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
