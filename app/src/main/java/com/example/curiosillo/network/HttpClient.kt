package com.example.curiosillo.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {

    /** Esegue una GET e restituisce il body come stringa. Lancia eccezione se != 200. */
    suspend fun get(url: String, timeoutMs: Int = 10_000): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod      = "GET"
                connectTimeout     = timeoutMs
                readTimeout        = timeoutMs
                setRequestProperty("Accept",     "application/json")
                setRequestProperty("User-Agent", "Curiosillo-Android/1.0")
            }
            try {
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK)
                    throw Exception("HTTP $code da $url")
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
}
