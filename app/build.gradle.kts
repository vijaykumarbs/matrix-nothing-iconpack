import java.util.Properties

plugins {
    id("com.android.application")
}

/*
 * Eight shipped styles. Every style is a separate install so that any launcher
 * (Nova, Lawnchair, Smart, Niagara, Hyperion, Kvaesitso, Nothing OS 5) can read
 * one static `appfilter.xml` - the format launchers actually support.
 */
val styleIds = listOf(
    "phosphorGlass", "phosphorFlat",
    "emberGlass", "emberFlat",
    "iceGlass", "iceFlat",
    "amberGlass", "amberFlat",
)

val styleNames = mapOf(
    "phosphorGlass" to "Phosphor Glass",
    "phosphorFlat" to "Phosphor Flat",
    "emberGlass" to "Ember Glass",
    "emberFlat" to "Ember Flat",
    "iceGlass" to "Ice Glass",
    "iceFlat" to "Ice Flat",
    "amberGlass" to "Amber Glass",
    "amberFlat" to "Amber Flat",
)

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun prop(key: String, env: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val storeFilePath = prop("storeFile", "MATRIX_KEYSTORE")
val storePasswordValue = prop("storePassword", "MATRIX_KEYSTORE_PASSWORD")
val keyAliasValue = prop("keyAlias", "MATRIX_KEY_ALIAS")
val keyPasswordValue = prop("keyPassword", "MATRIX_KEY_PASSWORD")
val hasSigning = storeFilePath != null && storePasswordValue != null &&
    keyAliasValue != null && keyPasswordValue != null

android {
    namespace = "dev.vijaykumarbs.matrix"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "dev.vijaykumarbs.matrix"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "REPO_URL", "\"https://github.com/vijaykumarbs/matrix-nothing-iconpack\"")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = rootProject.file(storeFilePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    flavorDimensions += "style"

    productFlavors {
        styleIds.forEach { id ->
            create(id) {
                dimension = "style"
                applicationIdSuffix = "." + id.lowercase()
                buildConfigField("String", "STYLE_ID", "\"$id\"")
                buildConfigField("String", "STYLE_NAME", "\"${styleNames[id]}\"")
                buildConfigField("boolean", "STYLE_GLASS", id.endsWith("Glass").toString())
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        // Vectors only: no density splits, no compressed junk.
        generateLocaleConfig = false
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/*.version",
            "META-INF/*.kotlin_module",
            "DebugProbesKt.bin",
        )
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

/*
 * GitHub release assets must be predictable: matrix-<slug>.apk. The in-app
 * download links point at these exact names.
 */
androidComponents {
    onVariants(selector().all()) { variant ->
        val flavor = variant.flavorName ?: return@onVariants
        val slug = flavor.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()
        variant.outputs.forEach { output ->
            (output as com.android.build.api.variant.impl.VariantOutputImpl)
                .outputFileName.set("matrix-$slug.apk")
        }
    }
}
