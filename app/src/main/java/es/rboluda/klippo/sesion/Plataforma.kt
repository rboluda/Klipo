package es.rboluda.klippo.sesion

import android.net.Uri

// Plataformas que admiten iniciar sesión para descargar contenido que lo exige
enum class Plataforma(
    val nombre: String,
    val urlLogin: String,
    // URL de la que se leen las cookies del WebView
    val urlCookies: String,
    val dominios: List<String>,
    // Cookie que solo existe con la sesión iniciada
    val cookieSesion: String,
    val archivo: String,
) {
    INSTAGRAM(
        nombre = "Instagram",
        urlLogin = "https://www.instagram.com/accounts/login/",
        urlCookies = "https://www.instagram.com",
        dominios = listOf("instagram.com"),
        cookieSesion = "sessionid",
        archivo = "instagram.txt",
    ),
    X(
        nombre = "X",
        urlLogin = "https://x.com/i/flow/login",
        urlCookies = "https://x.com",
        dominios = listOf("x.com", "twitter.com"),
        cookieSesion = "auth_token",
        archivo = "x.txt",
    );

    companion object {
        fun deUrl(url: String): Plataforma? {
            val host = Uri.parse(url.trim()).host?.lowercase() ?: return null
            return entries.firstOrNull { p -> p.dominios.any { host == it || host.endsWith(".$it") } }
        }
    }
}
