package com.example.curiosillo.network

import org.json.JSONObject

// ── Modelli ───────────────────────────────────────────────────────────────────

data class RemoteContent(
    val version:   Int,
    val curiosita: List<RemoteCuriosita>
)

data class RemoteCuriosita(
    val id:        String,
    val titolo:    String,
    val corpo:     String,
    val categoria: String,
    val emoji:     String,
    val quiz:      RemoteQuiz?
)

data class RemoteQuiz(
    val domanda:          String,
    val rispostaCorretta: String,
    val risposteErrate:   List<String>,   // sempre 3 elementi
    val spiegazione:      String
)

data class ReleaseInfo(
    val tagName:     String,   // es. "v1.3"
    val downloadUrl: String,   // URL diretto all'APK
    val releaseUrl:  String    // pagina web della release
)

// ── Parser (usa solo org.json già incluso in Android) ─────────────────────────

fun parseRemoteContent(json: String): RemoteContent {
    val root    = JSONObject(json)
    val version = root.getInt("version")
    val arr     = root.getJSONArray("curiosita")

    val lista = (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val quiz = if (obj.has("quiz")) {
            val q      = obj.getJSONObject("quiz")
            val errate = q.getJSONArray("risposte_errate")
            RemoteQuiz(
                domanda          = q.getString("domanda"),
                rispostaCorretta = q.getString("risposta_corretta"),
                risposteErrate   = (0 until errate.length()).map { errate.getString(it) },
                spiegazione      = q.getString("spiegazione")
            )
        } else null

        RemoteCuriosita(
            id        = obj.getString("id"),
            titolo    = obj.getString("titolo"),
            corpo     = obj.getString("corpo"),
            categoria = obj.getString("categoria"),
            emoji     = obj.getString("emoji"),
            quiz      = quiz
        )
    }
    return RemoteContent(version, lista)
}

fun parseReleaseInfo(json: String): ReleaseInfo? = try {
    val root    = JSONObject(json)
    val tag     = root.getString("tag_name")
    val webUrl  = root.getString("html_url")
    val assets  = root.getJSONArray("assets")
    val apkUrl  = if (assets.length() > 0)
        assets.getJSONObject(0).getString("browser_download_url")
    else ""
    ReleaseInfo(tag, apkUrl, webUrl)
} catch (e: Exception) {
    null
}
