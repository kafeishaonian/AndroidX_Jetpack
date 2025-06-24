plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    kotlin("kapt")
}

android {
    namespace = "com.aj.mvvm"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {

        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.toString()
    }
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // kotlin
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.androidx.core.ktx)

    //lifecycle
    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.common.java8)
    api(libs.lifecycle.extensions)

    //viewModel
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.fragment.ktx)
    // liveData
    api(libs.lifecycle.livedata.ktx)
    //navigation
    api(libs.navigation.fragment.ktx)
    api(libs.navigation.ui.ktx)
    //retrofit
    api(libs.retrofit)
    api(libs.converter.gson)

    implementation(libs.activity.ktx)

    api(libs.compiler.compiler)
    api(libs.compiler.ui)
    api(libs.activity.compose)
    api(libs.runtime.runtim)
    api(libs.material3.material3)
    api(libs.ui.uitooling)
    api(libs.material.icons.extended)

//    implementation(project(":Thread-P2P-Module"))
    implementation(project(":Router-Module"))
}