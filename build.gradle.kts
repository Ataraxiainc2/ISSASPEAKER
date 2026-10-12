// Build-time only: the Android Gradle Plugin brings these libraries in with known vulnerabilities (found by
// OSV through OpenSSF Scorecard). None of them reaches the app, but the build runs on the developer's PC and in
// CI, so they are raised to fixed releases here. Remove each line once AGP itself ships a fixed version.
buildscript {
    dependencies {
        constraints {
            listOf("bcprov", "bcpkix", "bcutil").forEach {
                classpath("org.bouncycastle:$it-jdk18on:1.86") {
                    because("GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26, GHSA-wg6q-6289-32hp, GHSA-c3fc-8qff-9hwx")
                }
            }
            classpath("org.bitbucket.b_c:jose4j:0.9.7") { because("GHSA-3677-xxcr-wjqv") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("GHSA-2363-cqg2-863c") }
            classpath("org.apache.commons:commons-lang3:3.21.0") { because("GHSA-j288-q9x7-2f5v") }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Kotlin static analysis that neither the compiler nor Android Lint covers: dead code, overly complex functions,
// swallowed exceptions, naming. Same version, rules and runner as ISSASTUDENT, EmuGaMind and ISSAGRAPPH.
// Run:  ./gradlew detekt        (rewrite formatting: ./gradlew detekt -PdetektAutoCorrect)
//
// Runs the official detekt-cli instead of the Gradle plugin: plugin 2.0.0-alpha.6 calls Configuration.setVisible,
// deprecated in Gradle 9.8, and would leave a "Deprecated Gradle features" warning on every build.

val detektTool = configurations.dependencyScope("detektTool")
val detektToolClasspath = configurations.resolvable("detektToolClasspath") { extendsFrom(detektTool.get()) }

dependencies {
    "detektTool"(libs.detekt.cli)
    "detektTool"(libs.detekt.formatting)
}

val detektSources = listOf("app/src/main/java", "app/src/test/java", "app/src/androidTest/java")
    .map { rootDir.resolve(it) }.filter { it.exists() }

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Kotlin static analysis with detekt (config/detekt.yml). Fails on any finding: there is no baseline."
    mainClass.set("dev.detekt.cli.Main")
    classpath = detektToolClasspath.get()
    val reports = layout.buildDirectory.dir("reports/detekt").get().asFile
    inputs.files(detektSources).withPropertyName("sources")
    inputs.files("config/detekt.yml").withPropertyName("config")
    outputs.dir(reports)
    detektSources.forEach { args("--input", it.path) }
    args(
        "--config", rootDir.resolve("config/detekt.yml").path,
        "--build-upon-default-config",
        "--parallel",
        "--base-path", rootDir.path,
        "--report", "html:${reports.resolve("detekt.html").path}",
        "--report", "sarif:${reports.resolve("detekt.sarif").path}",
    )
    if (providers.gradleProperty("detektAutoCorrect").isPresent) args("--auto-correct")
    // detekt embeds the Kotlin compiler, which still uses sun.misc.Unsafe; JDK 23+ prints four warning lines
    // for it on every run. The flag does not exist before JDK 23, so it is only passed there.
    if (JavaVersion.current() >= JavaVersion.VERSION_23) jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
