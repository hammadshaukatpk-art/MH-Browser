plugins { id("com.android.application") }
android {
    namespace = "com.mh.browser"
    compileSdk = 35
    defaultConfig { applicationId = "com.mh.browser"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"; ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") } }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation("androidx.webkit:webkit:1.13.0")
    implementation("androidx.core:core:1.13.1")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
}
