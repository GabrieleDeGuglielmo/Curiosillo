package com.example.curiosillo.firebase

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

        // Applica solo se il cloud ha dati più avanzati
        if (xpCloud > xpLocale) {
            val delta = xpCloud - xpLocale
            gamifPrefs.aggiungiXp(delta)
        }
        // streak: non sovrascriviamo la streak locale attiva
        // (potrebbe essere già avanzata nella sessione corrente)
        if (streakMaxCloud > streakMaxLocale) {
            // reset e reimpostazione non è disponibile direttamente —
            // gestiamo questo caso solo al primo avvio su nuovo dispositivo
        }
    }
}
