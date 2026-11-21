plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}


val appVersion = providers.gradleProperty("appVersion").orElse("1.0.0").get()
val appVersionParts = appVersion.split(".").map(String::toInt)
require(appVersionParts.size == 3 && appVersionParts.all { it in 0..999 }) {
    "appVersion must use MAJOR.MINOR.PATCH with each part between 0 and 999"
}
val computedVersionCode = appVersionParts[0] * 1_000_000 + appVersionParts[1] * 1_000 + appVersionParts[2]


android {
    namespace = "kr.stonecold.exkeyko"
    compileSdk = 37
    compileSdkMinor = 1
    defaultConfig {
        applicationId = "kr.stonecold.exkeyko"
        minSdk = 33
        targetSdk = 37
        versionCode = computedVersionCode
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            // 지원하는 ABI 설정 (arm, x86 등)
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                cppFlags.add("-std=c++11")  // C++11 표준 사용
                cFlags.add("-Wno-tautological-constant-out-of-range-compare")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")  // CMakeLists.txt 파일 경로
        }
    }

    signingConfigs {
        create("release") {
            val releaseStore = providers.gradleProperty("keystoreFile")
                .map(::File)
                .orElse(File(projectDir, "release-keystore.jks"))
                .get()
            storeFile = releaseStore
            storePassword = System.getenv("KEY_STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        warningsAsErrors = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.preference.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}
