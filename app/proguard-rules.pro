# Reglas de R8 para la versión publicada de Klippo.
# Criterio conservador: se recortan las librerías, pero no se toca lo que se usa por nombre
# o por reflexión, porque no hay pruebas automáticas que detecten una rotura.

# Código de Klippo entero y sin renombrar: los nombres de los enum se guardan en los ajustes
# (Calidad, Vista, Tema) y en los intents (Plataforma), y los parámetros de edición se
# serializan al girar la pantalla.
-keep class es.rboluda.klippo.** { *; }

# Motor de descarga (youtubedl-android): arranca Python y FFmpeg y lee su JSON por reflexión
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.compress.** { *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.brotli.dec.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.objectweb.asm.**

# Mantener la información de las excepciones para poder leer un fallo si llega a haberlo
-keepattributes SourceFile,LineNumberTable
