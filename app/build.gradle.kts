plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.testposturai" // REVISA QUE SIGUI EL TEU
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.testposturai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21" // Canvia "21" per "17"
    }
}

dependencies {
    // NOMÉS el que necessitem per a la IA i la càmera
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // TensorFlow i Càmera
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation(libs.androidx.monitor)
    testImplementation(libs.junit.junit)
}