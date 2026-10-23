import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// AGP 9 trae Kotlin integrado: no hace falta aplicar org.jetbrains.kotlin.android
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Marca de compilación (fecha y hora). Al ser un ValueSource, Gradle la vuelve a
// calcular en cada build aunque esté activa la configuration cache.
abstract class MarcaCompilacion : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmm"))
}

val versionBase = "0.1.0"
val marca = providers.of(MarcaCompilacion::class) {}.get()
// Cada compilación tiene su propia versión: "0.1.0-20261023.1305"
val nombreVersion = "$versionBase-$marca"
// versionCode = minutos desde 2026-01-01: siempre crece, así Android lo trata como actualización
val codigoVersion = Duration.between(
    LocalDateTime.of(2026, 1, 1, 0, 0),
    LocalDateTime.parse(marca, DateTimeFormatter.ofPattern("yyyyMMdd.HHmm")),
).toMinutes().toInt()

android {
    namespace = "es.rboluda.klippo"
    compileSdk = 37

    defaultConfig {
        applicationId = "es.rboluda.klippo"
        minSdk = 33
        targetSdk = 37
        versionCode = codigoVersion
        versionName = nombreVersion

        // Solo móviles de 64 bits: el motor (Python + FFmpeg) pesa mucho por ABI
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // Firma de las releases publicadas en GitHub. Las claves viven fuera del repo, en
    // ~/.gradle/gradle.properties (KLIPPO_KEYSTORE, KLIPPO_KEYSTORE_PASSWORD, KLIPPO_KEY_ALIAS,
    // KLIPPO_KEY_PASSWORD). Sin ellas, la release sale sin firmar y no se puede instalar.
    val keystore = providers.gradleProperty("KLIPPO_KEYSTORE").orNull
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.gradleProperty("KLIPPO_KEYSTORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("KLIPPO_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("KLIPPO_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
            // Sin minificar por ahora; se revisará cuando el motor esté integrado
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Para mostrar BuildConfig.VERSION_NAME en la pantalla de inicio
        buildConfig = true
    }

    // El motor ejecuta binarios nativos desde disco: las .so deben extraerse al instalar
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Copia el APK a <proyecto>/apk/ con la versión en el nombre
// (klippo-0.1.0-20261023.1305-debug.apk), para que el móvil no confunda archivos.
// Sync deja en esa carpeta solo el último de cada tipo (debug / release).
listOf("debug", "release").forEach { tipo ->
    val otro = if (tipo == "debug") "release" else "debug"
    val copiar = tasks.register<Sync>("copiarApk${tipo.replaceFirstChar { it.uppercase() }}") {
        // Copia local: la configuration cache no admite referencias al script dentro de la tarea
        val nombreApk = "klippo-$nombreVersion-$tipo.apk"
        from(layout.buildDirectory.dir("outputs/apk/$tipo")) {
            include("*.apk")
            rename { nombreApk }
        }
        into(rootProject.layout.projectDirectory.dir("apk"))
        preserve { include("*-$otro.apk") }
    }
    tasks.matching { it.name == "assemble${tipo.replaceFirstChar { it.uppercase() }}" }
        .configureEach { finalizedBy(copiar) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)

    // Reproductor del editor (previsualización del vídeo y del recorte)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Motor de descarga: yt-dlp (con Python) y FFmpeg empaquetados
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
