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
