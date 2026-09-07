import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val societySigningProperties =
    Properties().apply {

        val signingFile =
            File(
                "/home/server/.config/societybots/signing.properties"
            )

        if (!signingFile.exists()) {
            throw GradleException(
                "Society Bots signing.properties não encontrado."
            )
        }

        signingFile
            .inputStream()
            .use {
                load(it)
            }
    }


android {
    namespace = "opnet.fsocietydevs.devbots"
    compileSdk = 35

    defaultConfig {
        applicationId = "opnet.fsocietydevs.devbots"

        minSdk = 26
        targetSdk = 35

        versionCode = 8
        versionName = "1.1.3"
    }

    signingConfigs {

        create("societyPermanent") {

            storeFile =
                file(
                    societySigningProperties
                        .getProperty(
                            "storeFile"
                        )
                )

            storePassword =
                societySigningProperties
                    .getProperty(
                        "storePassword"
                    )

            keyAlias =
                societySigningProperties
                    .getProperty(
                        "keyAlias"
                    )

            keyPassword =
                societySigningProperties
                    .getProperty(
                        "keyPassword"
                    )
        }
    }


    buildTypes {

        getByName("debug") {

            signingConfig =
                signingConfigs
                    .getByName(
                        "societyPermanent"
                    )
        }
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(
        platform("androidx.compose:compose-bom:2024.12.01")
    )

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.10.0"
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
    )


    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    implementation(
        "io.coil-kt:coil-compose:2.7.0"
    )


    implementation(
        "com.google.zxing:core:3.5.3"
    )

}
