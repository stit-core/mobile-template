plugins {
    alias(libs.plugins.stit.jvm.library)
}

dependencies {
    compileOnly(libs.lint.api)
}

// Без этого атрибута Lint не видит реестр и считает jar невалидным.
tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "tech.stit.mobile.android.lint.StitIssueRegistry")
    }
}
