plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Configure detekt
detekt {
    toolVersion = "1.23.6"
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = project.file("$projectDir/config/detekt/baseline.xml")
    buildUponDefaultConfig = true
    allRules = false
}

// Configure ktlint
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("0.50.0")
    debug.set(true)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}
