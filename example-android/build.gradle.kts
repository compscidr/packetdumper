plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jasonernst.packetdumper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jasonernst.packetdumper"
        minSdk = 29
        targetSdk = 35
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
    implementation(project(":packetdumper"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.preference)
    implementation(libs.bundles.compose)
    implementation(libs.compose.ui.tooling)
    implementation(libs.accompanist.permissions)

    // excludes are here so we use the local project version for compiling rather than previously
    // packaged releases of this library which each of these other libraries depend on
    implementation(libs.icmp.common) {
        exclude(group = "com.jasonernst.packetdumper", module = "packetdumper")
    }
    implementation(libs.icmp.android) {
        exclude(group = "com.jasonernst.packetdumper", module = "packetdumper")
        // turn off logback-android to make things go faster
        // exclude(group = "com.github.tony19", module = "logback-android")
    }
    implementation(libs.kanonproxy) {
        exclude(group = "com.jasonernst.packetdumper", module = "packetdumper")
        // turn off logback-android to make things go faster
        // exclude(group = "com.github.tony19", module = "logback-android")
    }
    // turn off logback-android to make things go faster
    implementation(libs.logback.android)
    implementation(libs.material) // required for the themes.xml
    implementation(libs.slf4j.api)

    debugImplementation(libs.compose.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.test.runtime)
}