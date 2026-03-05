package com.example.curiosillo.network

/**
 * Controlla se c'è una nuova versione dell'app su GitHub Releases.
 * Confronta il tag della release (es. "v1.3") con BuildConfig.VERSION_NAME.
 *
 * IMPORTANTE: in build.gradle assicurati che versionName segua il formato "1.3"
 * e che le release GitHub abbiano tag del tipo "v1.3".
 */
class AppUpdateService {

    sealed class UpdateResult {
        data class AggiornamentoDisponibile(
            val versione:    String,
            val downloadUrl: String,
            val releaseUrl:  String
        ) : UpdateResult()
        object AppAggiornata   : UpdateResult()
        data class Errore(val messaggio: String) : UpdateResult()
    }

    suspend fun checkUpdate(versioneCorrente: String): UpdateResult {
        return try {
            val json    = HttpClient.get(RemoteConfig.GITHUB_RELEASES_API)
            val release = parseReleaseInfo(json) ?: return UpdateResult.Errore("Risposta non valida")

            // Rimuove il prefisso "v" dal tag (es. "v1.3" → "1.3")
            val versioneRemota = release.tagName.trimStart('v')

            if (isVersioneNuova(versioneCorrente, versioneRemota)) {
                UpdateResult.AggiornamentoDisponibile(
                    versione    = versioneRemota,
                    downloadUrl = release.downloadUrl,
                    releaseUrl  = release.releaseUrl
                )
            } else {
                UpdateResult.AppAggiornata
            }
        } catch (e: Exception) {
            UpdateResult.Errore(e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Confronta versioni nel formato "MAJOR.MINOR" o "MAJOR.MINOR.PATCH".
     * Restituisce true se remota > locale.
     */
    private fun isVersioneNuova(locale: String, remota: String): Boolean {
        val partiLocale  = locale.split(".").mapNotNull { it.toIntOrNull() }
        val partiRemota  = remota.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen       = maxOf(partiLocale.size, partiRemota.size)
        for (i in 0 until maxLen) {
            val l = partiLocale.getOrElse(i) { 0 }
            val r = partiRemota.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
