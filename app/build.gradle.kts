plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("jacoco")
    id("org.sonarqube") version "4.4.1.3373"
    id("com.github.spotbugs") version "5.0.14"
    id("org.owasp.dependencycheck") version "8.4.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

android {
    namespace = "com.example.nasonly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nasonly"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Room allowMainThreadQueries 仅 debug
            isTestCoverageEnabled = true
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles("benchmark-rules.pro")
            isMinifyEnabled = true
            isDebuggable = false
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // ExoPlayer
    implementation(libs.exoPlayer)
    
    // SMB
    implementation(libs.smbj)
    
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Coil for image loading
    implementation(libs.coil.compose)

    // Test Orchestrator
    androidTestUtil("androidx.test:orchestrator:1.4.2")

    // Benchmark
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.0")
    
    // Test dependencies
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

// Jacoco Test Coverage Configuration
jacoco {
    toolVersion = "0.8.8"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/Hilt_*.*",
        "**/*_HiltModules*.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector*.*",
        "**/DI/**/*.*"
    )

    val debugTree = fileTree("${buildDir}/intermediates/classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// SonarQube Configuration
sonarqube {
    properties {
        property("sonar.projectName", "NAS Player")
        property("sonar.projectKey", "nasplayer")
        property("sonar.language", "kotlin")
        property("sonar.sources", "src/main")
        property("sonar.tests", "src/test,src/androidTest")
        property("sonar.java.binaries", "build/intermediates/classes/debug")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.exclusions", "**/R.class,**/R$*.class,**/BuildConfig.*,**/Manifest*.*,**/*Test*.*,android/**/*.*")
    }
}

// SpotBugs Configuration
spotbugs {
    ignoreFailures.set(false)
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    excludeFilter.set(file("${rootProject.projectDir}/config/spotbugs/exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") {
            required.set(true)
            outputLocation.set(file("${buildDir}/reports/spotbugs/spotbugs.html"))
            setStylesheet("fancy-hist.xsl")
        }
        create("xml") {
            required.set(true)
            outputLocation.set(file("${buildDir}/reports/spotbugs/spotbugs.xml"))
        }
    }
}

// Detekt Configuration
detekt {
    config.setFrom(file("${rootProject.projectDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

// Dependency Check Configuration
dependencyCheck {
    format = "HTML"
    outputDirectory = "${buildDir}/reports"
    suppressionFile = "${rootProject.projectDir}/config/dependency-check/suppressions.xml"
    failBuildOnCVSS = 7.0f
    analyzers.apply {
        assemblyEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
    }
}
