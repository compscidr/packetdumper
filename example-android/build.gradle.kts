plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jasonernst.packetdumper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jasonernst.packetdumper"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.preference)
    implementation(libs.bundles.compose)
    implementation(libs.compose.ui.tooling)
    implementation(libs.accompanist.permissions)

    implementation(libs.icmp.common)
    implementation(libs.icmp.android)
    implementation(libs.kanonproxy)
    implementation(libs.knet)
    implementation(libs.logback.android)
    implementation(libs.material) // required for the themes.xml
    implementation(libs.slf4j.api)

    debugImplementation(libs.compose.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)
}