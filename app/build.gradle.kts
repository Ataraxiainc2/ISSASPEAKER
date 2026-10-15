import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
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
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Web OAuth client of the Google Cloud project "issaspeaker", used by Sign in with Google. A client ID is a
        // public identifier (it ships in every APK), not a secret; the value lives in gradle.properties.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${providers.gradleProperty("issa.googleWebClientId").getOrElse("")}\"",
        )
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
    // <signing dir>/issaspeaker-upload.properties. The signing dir is -Pissa.signingDir, else the
    // ISSASODA_SIGNING_DIR variable, else D:/SC/AK/signing (the owner's PC); -Pissa.uploadKey names the file
    // directly. A relative storeFile is resolved next to that file. Without it the release build is unsigned.
    val signingDir = providers.gradleProperty("issa.signingDir")
        .orElse(providers.environmentVariable("ISSASODA_SIGNING_DIR"))
        .getOrElse("D:/SC/AK/signing")
    val uploadKey = file(
        providers.gradleProperty("issa.uploadKey").getOrElse("$signingDir/issaspeaker-upload.properties"),
    )
    signingConfigs {
        if (uploadKey.exists()) {
            create("upload") {
                val props = Properties().apply { uploadKey.inputStream().use { load(it) } }
                storeFile = uploadKey.parentFile.resolve(props.getProperty("storeFile"))
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    // Downloadable Google Fonts (Play Services provider), see ui/theme/FontPool.kt.
    implementation(libs.compose.ui.text.google.fonts)
    // Sign in with Google through Credential Manager (ui/account).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    // The Google profile photo in the top bar.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // AI providers (ai/): the live registry and the providers' REST APIs, provider logos (SVG), and the vault key's
    // end-to-end encrypted copy in Block Store.
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.svg)
    implementation(libs.play.services.blockstore)
    implementation(libs.kotlinx.coroutines.play.services)
    // The Google Home speaker: Cast discovery and sessions with the Default Media Receiver (speaker/).
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)
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

// Android Studio's Build > Make Project asks for :app:unitTestClasses and :app:androidTestClasses, which AGP 9 no
// longer creates (it has per-variant tasks only). These aggregates keep Make Project working.
tasks.register("unitTestClasses") { dependsOn("compileDebugUnitTestSources") }
tasks.register("androidTestClasses") { dependsOn("compileDebugAndroidTestSources") }
