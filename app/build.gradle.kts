plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

android {
    namespace = "com.lodgy.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lodgy.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.jbcrypt)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.coil.compose)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

val jacocoExclusions = listOf(
    // build/framework generated
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*_HiltModules*.*", "**/*_Factory.*", "**/*_Factory$*.*", "**/*_MembersInjector.*",
    "**/Hilt_*.*", "**/*_HiltComponents*.*", "**/DaggerHiltComponent*.*", "**/*Module_*Factory.*",
    "**/*_Impl.*", "**/*_Impl$*.*",
    "**/di/**",
    // Compose UI - not exercised by JVM unit tests, needs instrumented/Compose UI tests instead
    "**/*Screen.*", "**/*ScreenKt*.*", "**/ComposableSingletons*.*",
    "**/ui/theme/**", "**/ui/icons/**", "**/ui/nav/**",
    "**/PhotoPickerField*.*", "**/PinKeypad*.*", "**/EnumLabels*.*", "**/MoreScreen*.*",
    // Room DAOs (interfaces, no logic) and entities (plain data holders)
    "**/data/dao/**", "**/data/entity/**", "**/LodgyDatabase.*",
    "**/LodgyApplication.*", "**/MainActivity.*",
    // CoroutineWorker needs a real WorkerParameters/work-testing+Robolectric harness to construct;
    // not reachable from a plain JVM unit test. WorkScheduler (the request-building half) is covered.
    "**/InvoiceGenerationWorker*.*",
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates a JaCoCo coverage report from the debug unit tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoExclusions)
    }
    classDirectories.setFrom(files(kotlinClasses))
    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testDebugUnitTest")
    group = "verification"

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoExclusions)
    }
    classDirectories.setFrom(files(kotlinClasses))
    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
