# NF-08 — jvmTarget 21 у Kotlin при Java 17: ловит проверка KGP «Inconsistent JVM-target compatibility»
EXPECT="android unit tests"
TOUCHES="androidApp/lint/build.gradle.kts"
apply() {
    printf '\nkotlin {\n    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }\n}\n' >>"$TOUCHES"
}
