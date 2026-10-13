plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "ru.man2ll.mnemokot"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.man2ll.mnemokot"
        minSdk = 26 // Архитектурный переход приложения на Android 8.0+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Безопасное подключение модулей Clean Architecture ядра MnemoKot
    implementation(project(":core:crypto"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))

    // Базовый стек Jetpack Compose UI
    implementation(libs.androidx.core-ktx)
    implementation(libs.androidx.lifecycle-runtime-ktx)
    implementation(libs.androidx.activity-compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Безопасная декларативная навигация
    implementation(libs.androidx.navigation-compose)

    // Внедрение зависимостей Dagger Hilt
    implementation(libs.hilt-android)
    kapt(libs.hilt.compiler)

    // Тестирование
    testImplementation(libs.junit)
}
