plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    // expect/actual classes are used on purpose (PlatformStorage) - silences the Beta warning
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
    androidLibrary { namespace = "com.digitallifesystem.shared"; compileSdk = 37; minSdk = 26; androidResources.enable = true }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        }
    }
}

compose.resources { publicResClass = true; packageOfResClass = "com.digitallifesystem.shared.resources" }
