import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// AGP 9 内置 Kotlin：无需 org.jetbrains.kotlin.android 插件
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.gathers.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gathers.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Miuix — 组件库（Compose Multiplatform / MIUI 风格）
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)

    // Compose Multiplatform 运行时（与 miuix 0.9.3 内部依赖一致）
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.exifinterface)
}
