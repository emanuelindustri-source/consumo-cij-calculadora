import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma de distribuciÃ³n interna (keystore.properties; si no existe se usa la de depuraciÃ³n)
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val signingProps = Properties()
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { signingProps.load(it) }
}

android {
    namespace = "com.calculadora.consumocij"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.calculadora.consumocij"
        // Android 14 (API 34) como versiÃ³n mÃ­nima, segÃºn requisito
        minSdk = 34
        targetSdk = 34
        versionCode = 8
        versionName = "2.3.0"
        vectorDrawables { useSupportLibrary = true }

        // Datos que la app muestra y usa para buscar actualizaciones.
        // Al publicar una version nueva, tools/check_update.py comprueba que estos
        // valores coincidan exactamente con version.json.
        buildConfigField("String", "BUILD_DATE", "\"2026-11-04\"")
        buildConfigField(
            "String", "UPDATE_URL",
            "\"https://raw.githubusercontent.com/emanuelindustri-source/consumo-cij-calculadora/main/version.json\""
        )
        buildConfigField(
            "String", "UPDATE_URL_FALLBACK",
            "\"https://emanuelindustri-source.github.io/consumo-cij-calculadora/version.json\""
        )
        buildConfigField(
            "String", "UPDATE_URL_CDN",
            "\"https://cdn.jsdelivr.net/gh/emanuelindustri-source/consumo-cij-calculadora@main/version.json\""
        )
        buildConfigField(
            "String", "NOTES_URL",
            "\"https://emanuelindustri-source.github.io/consumo-cij-calculadora/notas.html\""
        )
        buildConfigField(
            "String", "RELEASES_URL",
            "\"https://github.com/emanuelindustri-source/consumo-cij-calculadora/releases/latest\""
        )
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
    }
    composeOptions {
        // compatible con Kotlin 1.9.24
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Pruebas unitarias del motor de cÃ¡lculo (JVM, sin dispositivo)
    testImplementation("junit:junit:4.13.2")
    // org.json real en las pruebas JVM (el SDK lo simula y no parsea)
    testImplementation("org.json:json:20240303")
}
