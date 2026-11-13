import com.android.build.gradle.api.ApkVariantOutput

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.hourlysteps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hourlysteps"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    applicationVariants.all {
        if (buildType.name == "debug") {
            outputs.all {
                (this as ApkVariantOutput).outputFileName = "HourlyStepsReminder.apk"
            }
        }
    }
}
