import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.nenoeldeeb.education.absencerecord"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.nenoeldeeb.education.absencerecord"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    signingConfigs {
        val props = Properties()
        val localProperties = File(rootDir, "signing.properties")
        if (localProperties.exists()) {
            props.load(FileInputStream(localProperties))
        }
        getByName("debug") {
            storeFile = file(props.getProperty("debug.store.file"))
            storePassword = props.getProperty("debug.store.password")
            keyAlias = props.getProperty("debug.key.alias")
            keyPassword = props.getProperty("debug.key.password")
        }
        create("release") {
            storeFile = file(props.getProperty("release.store.file"))
            storePassword = props.getProperty("release.store.password")
            keyAlias = props.getProperty("release.key.alias")
            keyPassword = props.getProperty("release.key.password")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDefault = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility(libs.versions.jvmTarget.get())
        sourceCompatibility(libs.versions.jvmTarget.get())
    }
    buildFeatures { compose = true }
    lint { abortOnError = false }
    testOptions { unitTests { all { it.useJUnitPlatform() } } }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

ktlint {
    version.set("1.0.1")
    android.set(true)
    verbose.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        exclude("**/generated/**")
    }
    kotlinScriptAdditionalPaths { include(fileTree("scripts/")) }
}

dependencies {

    // core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // implementation dependencies (alphabetical)
    implementation(libs.androidx.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.google.accompanist.pager)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.datetime)
    implementation(libs.jetbrains.kotlinx.serialization.json)

    // ksp
    ksp(libs.androidx.room.compiler)

    // debug implementations (alphabetical)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // androidTest implementations (alphabetical)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.jetbrains.kotlin.test)
    androidTestImplementation(libs.jetbrains.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.mockk.android)

    // test implementations (alphabetical)
    testImplementation(libs.jetbrains.kotlin.test)
    testImplementation(libs.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.junit6.jupiter)
    testImplementation(libs.mockk)

    // test runtime-only
    testRuntimeOnly(libs.junit6.launcher)
}

tasks.getByPath("preBuild").dependsOn("ktlintFormat")

tasks.named("clean").configure {
    doLast {
        rootDir
            .listFiles { file ->
                file.isFile && file.name.contains(Regex("hs_err_pid|replay_pid"))
            }
            ?.forEach { it.delete() }
    }
}