plugins {
    alias(libs.plugins.stit.jvm.library)
}

dependencies {
    compileOnly(libs.lint.api)

    // lint-api — compileOnly, тестам нужен явно; lint-checks тянет остальную инфраструктуру Lint.
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.checks)
    testImplementation(libs.lint.tests)
}

// Без этого атрибута Lint не видит реестр и считает jar невалидным.
tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "tech.stit.mobile.android.lint.StitIssueRegistry")
    }
}
