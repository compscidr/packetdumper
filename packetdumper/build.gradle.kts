plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    id("jacoco")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = false
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

jacoco {
    toolVersion = "0.8.12"
}

dependencies {
    api(libs.slf4j.api)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.logback.classic)
}
