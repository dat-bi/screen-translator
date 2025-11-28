plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    ratchetFrom("origin/main")

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
