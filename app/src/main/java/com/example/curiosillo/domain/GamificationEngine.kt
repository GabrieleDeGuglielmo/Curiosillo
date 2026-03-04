package com.example.curiosillo.domain

import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.first

data class RisultatoAzione(
    val xpGuadagnati:    Int                  = 0,
    val badgeSbloccati:  List<BadgeSbloccato> = emptyList(),
    val streakAumentata: Boolean              = false,
    val nuovoLivello:    Boolean              = false
)

class GamificationEngine(
    private val prefs: GamificationPreferences,
    private val repo:  CuriosityRepository
) {

    // Chiamato quando l'utente preme "Ho imparato!"
    suspend fun onPillolaLetta(): RisultatoAzione {
        var xp = 10
        val badgeSbloccati = mutableListOf<BadgeSbloccato>()

        val streakAumentata = prefs.registraLettura()
        val streak          = prefs.streakCorrente.first()
        if (streakAumentata) xp += 15

        val xpPrima = prefs.xpTotali.first()
        prefs.aggiungiXp(xp)
        val xpDopo = prefs.xpTotali.first()

        val giaSbloccati  = repo.idBadgeSbloccati()
        val pilloleLette  = repo.curiositàImparate()
        val bookmark      = repo.totaleBookmark()

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

        return RisultatoAzione(
            xpGuadagnati    = xp,
            badgeSbloccati  = badgeSbloccati,
            streakAumentata = streakAumentata,
            nuovoLivello    = livelloDopo > livelloPrima
        )
    }

    // Chiamato dopo ogni risposta al quiz
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

        return RisultatoAzione(
            xpGuadagnati   = xp,
            badgeSbloccati = badgeSbloccati,
            nuovoLivello   = livelloDopo > livelloPrima
        )
    }

    private fun controllaBadge(
        giaSbloccati: List<String>,
        id: String,
        condizione: () -> Boolean
    ): List<BadgeSbloccato> {
        if (id in giaSbloccati || !condizione()) return emptyList()
        val def = BadgeCatalogo.trovaPerId(id) ?: return emptyList()
        return listOf(BadgeSbloccato(
            id          = def.id,
            nome        = def.nome,
            descrizione = def.descrizione,
            icona       = def.icona
        ))
    }
}
