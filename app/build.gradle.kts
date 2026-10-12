import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

/**
 * The admin About screen reads the feature catalog and the security findings at run time. Both live
 * in docs/ (the single source of truth, also rendered on GitHub); this task copies them into the APK's
 * assets so the screen can never drift from the repository.
 */
abstract class CopyProjectRecords : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val records: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().asFile
        target.deleteRecursively()
        target.mkdirs()
        records.files.forEach { it.copyTo(target.resolve(it.name)) }
    }
}

val copyProjectRecords = tasks.register<CopyProjectRecords>("copyProjectRecords") {
    records.from(
        rootProject.file("docs/features/catalog.tsv"),
        rootProject.file("docs/security/findings.tsv"),
    )
    outputDir.set(layout.buildDirectory.dir("generated/projectRecords"))
}

android {
    namespace = "com.issasoda.issaspeaker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.issasoda.issaspeaker"
        // Android 16+ only, on purpose: the app is built for the Galaxy S25 Ultra and uses the newest APIs freely.
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // locales_config.xml is generated from the values-* folders (res/resources.properties sets en-US as default).
        generateLocaleConfig = true
    }

    // Upload key for Google Play: it lives outside the project (and any repository), in
    // ~/.issasoda-keys/issaspeaker-upload.properties. Without that file the release build is unsigned.
    val uploadKey = file(
        providers.gradleProperty("issa.uploadKey").orNull
            ?: "${System.getProperty("user.home")}/.issasoda-keys/issaspeaker-upload.properties",
    )
    signingConfigs {
        if (uploadKey.exists()) {
            create("upload") {
                val props = Properties().apply { uploadKey.inputStream().use { load(it) } }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // To try the optimized build on a phone: -Pissa.debugSignedRelease=true signs it with the debug key.
            if (providers.gradleProperty("issa.debugSignedRelease").orNull == "true") {
                signingConfig = signingConfigs.getByName("debug")
            } else if (uploadKey.exists()) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        // Zero warnings: every warning fails the build. Exceptions are documented one by one in lint.xml.
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        lintConfig = rootProject.file("lint.xml")
    }

    testOptions {
        unitTests.isReturnDefaultValues = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyProjectRecords, CopyProjectRecords::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    // Downloadable Google Fonts (Play Services provider), see ui/theme/FontPool.kt.
    implementation(libs.compose.ui.text.google.fonts)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    // Android Lint runs on its own classpath, which brings old Bouncy Castle, httpclient and commons-lang3 with
    // known vulnerabilities. Raised to fixed releases, for the same reason as the buildscript block in the root
    // build.gradle.kts. Nothing here reaches the app.
    constraints {
        listOf(
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.bouncycastle:bcutil-jdk18on:1.86",
            "org.apache.httpcomponents:httpclient:4.5.14",
            "org.apache.commons:commons-lang3:3.21.0",
        ).forEach { add("androidLintTool", it) { because("OSV / OpenSSF Scorecard: vulnerable version on the Lint classpath") } }
    }
}

kotlin {
    compilerOptions {
        // Zero warnings, like Lint and detekt: a deprecation or an unchecked cast fails the build.
        allWarningsAsErrors.set(true)
    }
}

composeCompiler {
    includeComposeMappingFile.set(false)
}
