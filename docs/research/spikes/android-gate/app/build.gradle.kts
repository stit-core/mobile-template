plugins {
    alias(libs.plugins.stit.android.application)
    alias(libs.plugins.stit.android.application.compose)
    alias(libs.plugins.stit.android.hilt)
}

android {
    namespace = "tech.stit.mobiletemplate"

    defaultConfig {
        applicationId = "tech.stit.mobiletemplate"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("releaseLocal") {
            // Ключ живёт вне репозитория; путь/пароли — из gradle.properties или env.
            val storePath =
                providers
                    .gradleProperty("stitKeystorePath")
                    .orElse(providers.environmentVariable("STIT_KEYSTORE_PATH"))
            if (storePath.isPresent) {
                storeFile = file(storePath.get())
                storePassword = providers.environmentVariable("STIT_KEYSTORE_PASSWORD").getOrElse("")
                keyAlias = providers.environmentVariable("STIT_KEY_ALIAS").getOrElse("")
                keyPassword = providers.environmentVariable("STIT_KEY_PASSWORD").getOrElse("")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("releaseLocal")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.feature.todos)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.konsist)
}
