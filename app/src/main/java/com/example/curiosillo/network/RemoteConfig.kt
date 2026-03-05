package com.example.curiosillo.network

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SETUP (da fare una volta sola):
 *
 * 1. Vai su https://gist.github.com
 *    → Crea un Gist pubblico col file curiosillo.json
 *    → Clicca "Raw" → copia l'URL e sostituiscilo in CONTENT_URL
 *    Esempio: https://gist.githubusercontent.com/mario/abc123/raw/curiosillo.json
 *
 * 2. Vai su https://github.com/new
 *    → Crea un repo pubblico chiamato "curiosillo"
 *    → Sostituisci TUOUSERNAME in GITHUB_REPO
 *    Esempio: "mario/curiosillo"
 *
 * Per distribuire aggiornamenti dell'app:
 *    → GitHub repo → Releases → "Draft a new release"
 *    → Allega l'APK → Pubblica
 *    L'app mostrerà automaticamente il dialog di aggiornamento.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object RemoteConfig {
    const val CONTENT_URL         = "https://gist.githubusercontent.com/GabrieleDeGuglielmo/35234b90c92871338d4f54942efb4e8b/raw/4011f54c430e025b732c5e8059c67aa8d2e0cc1c/curiosillo.json"
    const val GITHUB_REPO         = "GabrieleDeGuglielmo/curiosillo"
    const val GITHUB_RELEASES_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
}
