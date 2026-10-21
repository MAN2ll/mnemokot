dependencies {
    api(project(":core:crypto"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.androidx.biometric)
    
    implementation(libs.androidx.navigation.compose)
    implementation(project.provider { "androidx.hilt:hilt-navigation-compose:1.2.0" })

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
