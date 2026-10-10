plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Личный ключ подписи (androidApp/dls.keystore). Один и тот же ключ у всех сборок =>
// новую версию можно ставить поверх старой без потери данных. Если файла нет — сборка идёт без него.
val dlsKeystore = file("dls.keystore")

android {
    namespace = "com.digitallifesystem"
    compileSdk = 37
    buildFeatures {
        compose = true
    }
    defaultConfig {
        applicationId = "com.digitallifesystem"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "1.3.0"
    }
    if (dlsKeystore.exists()) {
        signingConfigs {
            create("dls") {
                storeFile = dlsKeystore
                storePassword = "digitallife"
                keyAlias = "dls"
                keyPassword = "digitallife"
            }
        }
        buildTypes {
            getByName("debug") { signingConfig = signingConfigs.getByName("dls") }
            getByName("release") { signingConfig = signingConfigs.getByName("dls") }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity:1.12.2")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.core:core:1.17.0")
}
