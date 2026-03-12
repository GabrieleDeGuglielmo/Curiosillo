package com.example.curiosillo.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class VersioneChangelog(
    val versione: String,
    val data:     String,
    val novita:   List<String>,
    val fix:      List<String> = emptyList()
)

class ChangelogService {

    private val url = "https://gist.githubusercontent.com/GabrieleDeGuglielmo/05fd6e74694b635a63c96f9af6866f81/raw/changelog.json"

    suspend fun scaricaChangelog(): List<VersioneChangelog> = withContext(Dispatchers.IO) {
        try {
            val json = java.net.URL("$url?t=${System.currentTimeMillis()}").readText()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj    = array.getJSONObject(i)
                val novita = obj.optJSONArray("novita") ?: JSONArray()
                val fix    = obj.optJSONArray("fix")    ?: JSONArray()
                VersioneChangelog(
                    versione = obj.getString("versione"),
                    data     = obj.optString("data", ""),
                    novita   = (0 until novita.length()).map { novita.getString(it) },
                    fix      = (0 until fix.length()).map    { fix.getString(it)    }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}