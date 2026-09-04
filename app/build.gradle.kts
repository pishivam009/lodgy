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
        buildConfig = true
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
    "**/FilterChipRow*.*", "**/StatusBadge*.*", "**/AppRoot.*", "**/AppRootKt*.*", "**/AuthIcons*.*", "**/ContactIcons*.*",
    "**/StatusIcons*.*",
    // Room DAOs (interfaces, no logic) and entities (plain data holders)
    "**/data/dao/**", "**/data/entity/**", "**/LodgyDatabase.*",
    "**/LodgyApplication.*", "**/MainActivity.*",
    // Migrations are raw SQL against a real SQLite file - they need an instrumented
    // MigrationTestHelper, not a JVM test. Each one's SQL is instead diffed against Room's own
    // exported schema JSON when it is written.
    "**/Migrations*.*",
    // Thin wrappers over Android graphics/notification/IO APIs with no branching of their own -
    // the logic they call was pulled out precisely so it could be tested here (PdfLayout,
    // VacancyNudge, DuesNudge, HistoryCsv).
    "**/LodgyPdfRenderer*.*", "**/LodgyNotifications*.*", "**/HistoryCsvReader*.*",
    "**/PhotoStorage*.*",
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
                // Measured 97.1% after LODGY-47. Set a few points below that: high enough that a
                // whole feature landing untested trips it, loose enough that one uncovered branch
                // does not block a build.
                counter = "LINE"
                minimum = "0.93".toBigDecimal()
            }
        }
    }
}
