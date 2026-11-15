import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val releaseVersionName = providers.gradleProperty("releaseVersionName")
val releaseVersionCode = providers.gradleProperty("releaseVersionCode")
require(releaseVersionName.isPresent == releaseVersionCode.isPresent) {
    "releaseVersionName and releaseVersionCode must be provided together"
}

// A simple KEY=value format: optional matching quotes, no variable expansion.
// Keep Windows path separators and password characters (including # and =) intact.
val localSigningValues = providers.fileContents(rootProject.layout.projectDirectory.file(".env"))
    .asText.orElse("").map { contents ->
        contents.lineSequence()
            .map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                require('=' in line) { "Invalid .env entry: expected KEY=value" }
                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim().let { raw ->
                    if (raw.length >= 2 &&
                        ((raw.first() == '"' && raw.last() == '"') ||
                            (raw.first() == '\'' && raw.last() == '\''))
                    ) raw.substring(1, raw.length - 1) else raw
                }
                key to value
            }.toMap()
    }

fun releaseSigningValue(name: String) = providers.environmentVariable(name)
    .orElse(localSigningValues.map { it[name] })

val releaseKeystorePath = releaseSigningValue("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = releaseSigningValue("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("ANDROID_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("ANDROID_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasReleaseSigning = releaseSigningValues.any { it.isPresent }
require(!hasReleaseSigning || releaseSigningValues.all { it.orNull?.isNotBlank() == true }) {
    "Release signing requires ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD in environment variables or the root .env file"
}

android {
    namespace = "org.exxlexxlee.svitlo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.exxlexxlee.svitlo"
        minSdk = 28
        targetSdk = 37
        versionCode = releaseVersionCode.map { value ->
            value.toInt().also { require(it in 1..2_100_000_000) { "Invalid releaseVersionCode" } }
        }.getOrElse(3)
        versionName = releaseVersionName.getOrElse("1.3")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
            extensions.configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            extensions.configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = providers.gradleProperty("uploadCrashlyticsMapping")
                    .map(String::toBoolean).getOrElse(false)
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.app.update)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
