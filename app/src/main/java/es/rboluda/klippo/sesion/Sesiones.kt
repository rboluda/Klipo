package es.rboluda.klippo.sesion

import android.content.Context
import android.webkit.CookieManager
import java.io.File

// Cookies de sesión en formato Netscape (el que entiende yt-dlp con --cookies).
// Se guardan en filesDir (privado y fuera de la caché, para que no se borren cada hora).
object Sesiones {

    private const val CARPETA = "cookies"
    private const val UN_ANIO_S = 365L * 24 * 60 * 60

    fun archivo(context: Context, plataforma: Plataforma): File =
        File(File(context.filesDir, CARPETA), plataforma.archivo)

    fun tieneSesion(context: Context, plataforma: Plataforma): Boolean =
        archivo(context, plataforma).exists()

    // Archivo de cookies a usar para descargar [url], si hay sesión guardada
    fun cookiesPara(context: Context, url: String): File? =
        Plataforma.deUrl(url)?.let { archivo(context, it) }?.takeIf { it.exists() }

    // Copia las cookies del WebView; devuelve false si aún no hay sesión iniciada
    fun guardarDesdeWebView(context: Context, plataforma: Plataforma): Boolean {
        val gestor = CookieManager.getInstance()
        gestor.flush()
        val cookies = leerCookies(gestor, plataforma)
        if (cookies.none { it.first == plataforma.cookieSesion }) return false

        // CookieManager no da la caducidad real: se pone un año y yt-dlp la refresca al usarlas
        val caducidad = System.currentTimeMillis() / 1000 + UN_ANIO_S
        val texto = buildString {
            append("# Netscape HTTP Cookie File\n")
            for (dominio in plataforma.dominios) {
                for ((nombre, valor) in cookies) {
                    append(".$dominio\tTRUE\t/\tTRUE\t$caducidad\t$nombre\t$valor\n")
                }
            }
        }
        archivo(context, plataforma).apply { parentFile?.mkdirs() }.writeText(texto)
        return true
    }

    // Borra el archivo y caduca las cookies de esa plataforma en el WebView
    fun cerrar(context: Context, plataforma: Plataforma) {
        archivo(context, plataforma).delete()
        val gestor = CookieManager.getInstance()
        for ((nombre, _) in leerCookies(gestor, plataforma)) {
            gestor.setCookie(plataforma.urlCookies, "$nombre=; Max-Age=0; Path=/")
            for (dominio in plataforma.dominios) {
                gestor.setCookie(plataforma.urlCookies, "$nombre=; Max-Age=0; Path=/; Domain=.$dominio")
            }
        }
        gestor.flush()
    }

    private fun leerCookies(gestor: CookieManager, plataforma: Plataforma): List<Pair<String, String>> =
        gestor.getCookie(plataforma.urlCookies).orEmpty()
            .split(";")
            .mapNotNull { parte ->
                val i = parte.indexOf('=')
                if (i <= 0) null else parte.substring(0, i).trim() to parte.substring(i + 1).trim()
            }
}
