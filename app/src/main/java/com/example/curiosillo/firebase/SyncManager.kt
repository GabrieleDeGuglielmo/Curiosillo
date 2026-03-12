package com.example.curiosillo.firebase

import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.first

/**
 * Al primo login sincronizza i progressi locali (XP, streak, badge, pillole lette)
 * su Firestore. Nelle sessioni successive mantiene i due in sync.
 */
class SyncManager(
    private val repo:         CuriosityRepository,
    private val gamifPrefs:   GamificationPreferences
) {

    /**
     * Chiamato una volta sola al primo login.
     * Carica i dati locali su Firestore senza sovrascrivere dati cloud esistenti.
     */
    suspend fun migraLocaleVersoCloud(uid: String) {
        val profiloCloud = FirebaseManager.caricaProfilo(uid)

        // XP e streak: prendi il massimo tra locale e cloud
        val xpLocale      = gamifPrefs.xpTotali.first()
        val streakLocale   = gamifPrefs.streakCorrente.first()
        val streakMaxLocale = gamifPrefs.streakMassima.first()

        val xpCloud        = (profiloCloud?.get("xp") as? Long)?.toInt() ?: 0
        val streakCloud    = (profiloCloud?.get("streakCorrente") as? Long)?.toInt() ?: 0
        val streakMaxCloud = (profiloCloud?.get("streakMassima") as? Long)?.toInt() ?: 0

        FirebaseManager.aggiornaProfilo(uid, mapOf(
            "xp"             to maxOf(xpLocale, xpCloud),
            "streakCorrente" to maxOf(streakLocale, streakCloud),
            "streakMassima"  to maxOf(streakMaxLocale, streakMaxCloud)
        ))

        // Badge: unione di locali e cloud
        val badgeLocali = repo.idBadgeSbloccati().toList()
        val badgeCloud  = FirebaseManager.caricaBadge(uid)
        val badgeTutti  = (badgeLocali + badgeCloud).distinct()
        if (badgeTutti.isNotEmpty()) {
            FirebaseManager.aggiornaProfilo(uid, emptyMap()) // no-op per ora
            badgeTutti.forEach { FirebaseManager.aggiungiBadge(uid, it) }
        }

        // Pillole lette: carica gli externalId letti localmente
        val pilloleCloud = FirebaseManager.caricaPilloleLette(uid)
        val pilloleLocali = repo.getPilloleLette()
            .mapNotNull { it.externalId }
            .filter { it !in pilloleCloud }
        pilloleLocali.forEach { FirebaseManager.aggiungiPillolaLetta(uid, it) }
    }

    /**
     * Sincronizza XP e streak su Firestore dopo ogni azione gamification.
     */
    suspend fun sincronizzaGamification(uid: String) {
        val xp           = gamifPrefs.xpTotali.first()
        val streak       = gamifPrefs.streakCorrente.first()
        val streakMassima = gamifPrefs.streakMassima.first()
        FirebaseManager.aggiornaProfilo(uid, mapOf(
            "xp"             to xp,
            "streakCorrente" to streak,
            "streakMassima"  to streakMassima
        ))
    }

    /**
     * Al login su un nuovo dispositivo, scarica i progressi dal cloud
     * e li applica in locale se sono più avanzati.
     */
    suspend fun ripristinaCloudVersoLocale(uid: String) {
        val profilo = FirebaseManager.caricaProfilo(uid) ?: return

        val xpCloud        = (profilo["xp"] as? Long)?.toInt() ?: 0
        val streakCloud    = (profilo["streakCorrente"] as? Long)?.toInt() ?: 0
        val streakMaxCloud = (profilo["streakMassima"] as? Long)?.toInt() ?: 0

        val xpLocale       = gamifPrefs.xpTotali.first()
        val streakLocale   = gamifPrefs.streakCorrente.first()
        val streakMaxLocale = gamifPrefs.streakMassima.first()

        // 1. Applica XP se cloud > locale
        if (xpCloud > xpLocale) {
            val delta = xpCloud - xpLocale
            gamifPrefs.aggiungiXp(delta)
        }
        
        // 2. Badge: Scarica dal cloud e inserisci nel DB locale se mancano
        val badgeCloud = FirebaseManager.caricaBadge(uid)
        val badgeLocaliIds = repo.idBadgeSbloccati()
        
        badgeCloud.forEach { id ->
            if (id !in badgeLocaliIds) {
                val def = BadgeCatalogo.trovaPerId(id)
                if (def != null) {
                    repo.sbloccaBadge(BadgeSbloccato(
                        id          = def.id,
                        nome        = def.nome,
                        descrizione = def.descrizione,
                        icona       = def.icona
                        // sbloccatoAt usa il default ora attuale
                    ))
                }
            }
        }
        
        // streak: gestiamo caso solo al primo avvio su nuovo dispositivo se necessario
        if (streakMaxCloud > streakMaxLocale) {
            // gamifPrefs non ha un setter diretto per streakMassima, ma potremmo implementarlo
        }
    }
}
