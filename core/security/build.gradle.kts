plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "ru.man2ll.mnemokot.core.security"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Подключаем модули данных и криптографии фраз
    implementation(project(":core:database"))
    implementation(project(":core:crypto"))

    // Core & DI
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Crypto & Security (Jetpack Security + встроенные примитивы Java)
    implementation(libs.androidx.security.crypto)
    
    // Для сериализации бэкапов в JSON
    implementation("applicationId" to Any()) // Используем стандартный кодировщик Android/Java java.io или org.json
}
