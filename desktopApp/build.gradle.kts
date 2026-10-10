plugins { id("org.jetbrains.kotlin.jvm"); id("org.jetbrains.compose"); id("org.jetbrains.kotlin.plugin.compose") }

dependencies { implementation(project(":shared")); implementation(compose.desktop.currentOs) }

compose.desktop { application { mainClass = "com.digitallifesystem.MainKt"; nativeDistributions { targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb); packageName = "DigitalLifeSystem"; packageVersion = "1.0.0" } } }
