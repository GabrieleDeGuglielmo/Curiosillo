package com.example.curiosillo.domain

import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.firebase.SyncManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.first

data class RisultatoAzione(
    val xpGuadagnati:    Int                  = 0,
    val badgeSbloccati:  List<BadgeSbloccato> = emptyList(),
    val streakAumentata: Boolean              = false,
    val nuovoLivello:    Boolean              = false
)

class GamificationEngine(
    private val prefs:        GamificationPreferences,
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences
) {
    private val syncManager = SyncManager(repo, prefs, contentPrefs)

    suspend fun onPillolaLetta(): RisultatoAzione {
        var xp = 10
        val badgeSbloccati = mutableListOf<BadgeSbloccato>()

        val streakAumentata = prefs.registraLettura()
        val streak          = prefs.streakCorrente.first()
        if (streakAumentata) xp += 15

        val xpPrima = prefs.xpTotali.first()
        prefs.aggiungiXp(xp)
        val xpDopo = prefs.xpTotali.first()

        val giaSbloccati = repo.idBadgeSbloccati()
        val pilloleLette = repo.curiositàImparate()
        val bookmark     = repo.totaleBookmark()

        badgeSbloccati += controllaBadge(giaSbloccati, "prima_pillola") { pilloleLette >= 1  }
        badgeSbloccati += controllaBadge(giaSbloccati, "pillole_10")    { pilloleLette >= 10 }
        badgeSbloccati += controllaBadge(giaSbloccati, "pillole_20")    { pilloleLette >= 20 }
        badgeSbloccati += controllaBadge(giaSbloccati, "pillole_50")    { pilloleLette >= 50 }
        badgeSbloccati += controllaBadge(giaSbloccati, "streak_3")      { streak >= 3  }
        badgeSbloccati += controllaBadge(giaSbloccati, "streak_7")      { streak >= 7  }
        badgeSbloccati += controllaBadge(giaSbloccati, "streak_30")     { streak >= 30 }
        badgeSbloccati += controllaBadge(giaSbloccati, "preferiti_5")   { bookmark >= 5 }

        val livelloPrima = LivelloHelper.daXp(xpPrima).numero
        val livelloDopo  = LivelloHelper.daXp(xpDopo).numero
        if (livelloDopo >= 3) badgeSbloccati += controllaBadge(giaSbloccati, "livello_3") { true }
        if (livelloDopo >= 5) badgeSbloccati += controllaBadge(giaSbloccati, "livello_5") { true }

        badgeSbloccati.forEach { repo.sbloccaBadge(it) }

        // Sync su Firebase se loggato
        FirebaseManager.uid?.let { uid ->
            syncManager.sincronizzaGamification(uid)
            badgeSbloccati.forEach { FirebaseManager.aggiungiBadge(uid, it.id) }
        }

        return RisultatoAzione(
            xpGuadagnati    = xp,
            badgeSbloccati  = badgeSbloccati,
            streakAumentata = streakAumentata,
            nuovoLivello    = livelloDopo > livelloPrima
        )
    }

    suspend fun onRispostaQuiz(corretta: Boolean): RisultatoAzione {
        val badgeSbloccati = mutableListOf<BadgeSbloccato>()
        val xp             = if (corretta) 20 else 0

        val risposteFila = prefs.aggiornaRisposteFila(corretta)
        val giaSbloccati = repo.idBadgeSbloccati()

        badgeSbloccati += controllaBadge(giaSbloccati, "prima_risposta") { true }
        if (corretta) {
            badgeSbloccati += controllaBadge(giaSbloccati, "perfetto_5") { risposteFila >= 5 }
        }

        val livelloPrima = LivelloHelper.daXp(prefs.xpTotali.first()).numero
        if (xp > 0) prefs.aggiungiXp(xp)
        val livelloDopo = LivelloHelper.daXp(prefs.xpTotali.first()).numero

        if (livelloDopo >= 3) badgeSbloccati += controllaBadge(giaSbloccati, "livello_3") { true }
        if (livelloDopo >= 5) badgeSbloccati += controllaBadge(giaSbloccati, "livello_5") { true }

        badgeSbloccati.forEach { repo.sbloccaBadge(it) }

        // Sync su Firebase se loggato
        FirebaseManager.uid?.let { uid ->
            syncManager.sincronizzaGamification(uid)
            badgeSbloccati.forEach { FirebaseManager.aggiungiBadge(uid, it.id) }
        }

        return RisultatoAzione(
            xpGuadagnati   = xp,
            badgeSbloccati = badgeSbloccati,
            nuovoLivello   = livelloDopo > livelloPrima
        )
    }

    suspend fun onScopertaEffettuata(numeroScoperte: Int, titolo: String): RisultatoAzione {
        val xp = 25
        val badgeSbloccati = mutableListOf<BadgeSbloccato>()
        
        val xpPrima = prefs.xpTotali.first()
        prefs.aggiungiXp(xp)
        val xpDopo = prefs.xpTotali.first()
        
        val giaSbloccati = repo.idBadgeSbloccati()
        
        // Badge standard scoperte
        badgeSbloccati += controllaBadge(giaSbloccati, "scoperta_1")  { numeroScoperte >= 1 }
        badgeSbloccati += controllaBadge(giaSbloccati, "scoperte_10") { numeroScoperte >= 10 }
        badgeSbloccati += controllaBadge(giaSbloccati, "scoperte_50") { numeroScoperte >= 50 }
        
        // --- Badge Leggendari (Monumenti) ---
        val t = titolo.lowercase()
        if ("colosseo" in t || "colosseum" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_colosseo") { true }
        if ("pantheon" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_pantheon") { true }
        if ("torre di pisa" in t || "leaning tower" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_pisa") { true }
        if ("duomo di milano" in t || "milan cathedral" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_milano") { true }
        if ("basilica di san pietro" in t || "st. peter's basilica" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_pietro") { true }
        if ("maria del fiore" in t || "florence cathedral" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_fiore") { true }
        if ("pompei" in t || "pompeii" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_pompei") { true }
        if ("san marco" in t || "st. mark's basilica" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_marco") { true }
        if ("reggia di caserta" in t || "royal palace of caserta" in t) badgeSbloccati += controllaBadge(giaSbloccati, "leg_caserta") { true }

        val livelloPrima = LivelloHelper.daXp(xpPrima).numero
        val livelloDopo  = LivelloHelper.daXp(xpDopo).numero
        
        badgeSbloccati.forEach { repo.sbloccaBadge(it) }
        
        FirebaseManager.uid?.let { uid ->
            syncManager.sincronizzaGamification(uid)
            badgeSbloccati.forEach { FirebaseManager.aggiungiBadge(uid, it.id) }
        }
        
        return RisultatoAzione(
            xpGuadagnati = xp,
            badgeSbloccati = badgeSbloccati,
            nuovoLivello = livelloDopo > livelloPrima
        )
    }

    private fun controllaBadge(
        giaSbloccati: Set<String>,
        id: String,
        condizione: () -> Boolean
    ): List<BadgeSbloccato> {
        if (id in giaSbloccati || !condizione()) return emptyList()
        val def = BadgeCatalogo.trovaPerId(id) ?: return emptyList()
        return listOf(BadgeSbloccato(id = def.id, nome = def.nome,
            descrizione = def.descrizione, icona = def.icona))
    }
}
