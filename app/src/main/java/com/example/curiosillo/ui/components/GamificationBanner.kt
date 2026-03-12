package com.example.curiosillo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curiosillo.domain.LivelloHelper
import kotlin.math.*

// Soglia giorni per XP streak massimi (badge streak_30)
private const val STREAK_MAX_GIORNI = 30

@Composable
fun GamificationBanner(
    xpTotali:       Int,
    streakCorrente: Int,
    modifier:       Modifier = Modifier
) {
    val livello      = LivelloHelper.daXp(xpTotali)
    val progressione = LivelloHelper.progressione(xpTotali)
    val xpMancanti   = LivelloHelper.xpAlProssimo(xpTotali)

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Livello ${livello.numero}",
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        livello.titolo,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
                StreakFlame(streakCorrente = streakCorrente)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$xpTotali XP", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                if (xpMancanti > 0)
                    Text("$xpMancanti XP al prossimo livello", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                else
                    Text("Livello massimo!", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { progressione },
                modifier   = Modifier.fillMaxWidth().height(8.dp),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFF333355)
            )
        }
    }
}

// ── Fiamma con scintille ──────────────────────────────────────────────────────

// Parametri di ogni particella scintilla — definiti staticamente per evitare
// ricalcoli a ogni recomposition
private data class ParticellaParams(
    val angleRad:    Float,   // direzione di volo
    val speed:       Float,   // velocità relativa (0..1)
    val size:        Float,   // raggio px relativo (0..1)
    val delayFrac:   Float,   // offset fase animazione (0..1)
    val colorIdx:    Int      // 0=giallo, 1=arancione, 2=bianco
)

private val PARTICELLE: List<ParticellaParams> = listOf(
    // sinistra-alto
    ParticellaParams(-2.4f, 0.85f, 0.30f, 0.00f, 0),
    ParticellaParams(-2.0f, 0.65f, 0.20f, 0.30f, 2),
    ParticellaParams(-1.8f, 0.95f, 0.25f, 0.60f, 1),
    // destra-alto
    ParticellaParams(-0.7f, 0.80f, 0.28f, 0.15f, 1),
    ParticellaParams(-0.9f, 0.60f, 0.18f, 0.45f, 0),
    ParticellaParams(-0.5f, 0.90f, 0.22f, 0.75f, 2),
    // diretto verso l'alto
    ParticellaParams(-1.57f, 1.00f, 0.35f, 0.20f, 0),
    ParticellaParams(-1.57f, 0.70f, 0.18f, 0.55f, 1),
    // leggermente laterali
    ParticellaParams(-2.7f, 0.75f, 0.22f, 0.10f, 2),
    ParticellaParams(-0.4f, 0.70f, 0.20f, 0.85f, 0),
    ParticellaParams(-1.2f, 0.88f, 0.26f, 0.40f, 1),
    ParticellaParams(-1.9f, 0.55f, 0.16f, 0.70f, 2),
)

private val COLORI_SCINTILLE = listOf(
    Color(0xFFFFD600), // giallo
    Color(0xFFFF6600), // arancione
    Color(0xFFFFFFFF), // bianco
)

@Composable
private fun StreakFlame(streakCorrente: Int) {
    val fill = (streakCorrente.toFloat() / STREAK_MAX_GIORNI).coerceIn(0f, 1f)
    val scintille = streakCorrente >= STREAK_MAX_GIORNI

    // Riempimento animato
    val fillAnim by animateFloatAsState(
        targetValue   = fill,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "flame_fill"
    )

    val infinite = rememberInfiniteTransition(label = "flame")

    // Progresso scintille: 0→1 in loop, usato per calcolare posizione particelle
    val sparkProgress by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spark"
    )

    val density   = LocalDensity.current
    val boxSizeDp = 52.dp
    val boxSizePx = with(density) { boxSizeDp.toPx() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier.size(boxSizeDp),
            contentAlignment = Alignment.Center
        ) {
            // ── Strato 1: fiamma grigia (base) ─────────────────────────────
            Canvas(modifier = Modifier.size(boxSizeDp)) {
                val paint = android.graphics.Paint().apply {
                    textSize    = boxSizePx * 0.82f
                    textAlign   = android.graphics.Paint.Align.CENTER
                    colorFilter = android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix().also { it.setSaturation(0f) }
                    )
                    alpha = 150
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "🔥",
                    size.width / 2f,
                    size.height * 0.82f,
                    paint
                )
            }

            // ── Strato 2: fiamma colorata, riempita dal basso ───────────────
            Canvas(modifier = Modifier.size(boxSizeDp)) {
                if (fillAnim > 0f) {
                    clipRect(
                        left   = 0f,
                        top    = size.height * (1f - fillAnim),
                        right  = size.width,
                        bottom = size.height
                    ) {
                        val paint = android.graphics.Paint().apply {
                            textSize  = boxSizePx * 0.82f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "🔥",
                            size.width / 2f,
                            size.height * 0.82f,
                            paint
                        )
                    }
                }
            }

            // ── Strato 3: scintille (solo a 30+ giorni) ─────────────────────
            if (scintille) {
                Canvas(modifier = Modifier.size(boxSizeDp)) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.45f  // punto di emissione: centro fiamma

                    PARTICELLE.forEach { p ->
                        // fase personalizzata per ogni particella
                        val fase = ((sparkProgress + p.delayFrac) % 1f)

                        // Le particelle compaiono nella prima metà, svaniscono nella seconda
                        val alpha = when {
                            fase < 0.15f -> fase / 0.15f          // fade in
                            fase < 0.6f  -> 1f                     // piena visibilità
                            else         -> 1f - (fase - 0.6f) / 0.4f  // fade out
                        }.coerceIn(0f, 1f)

                        // Distanza percorsa: più lontana man mano che la fase avanza
                        val distanza = fase * boxSizePx * 0.9f * p.speed

                        val px = cx + cos(p.angleRad) * distanza
                        val py = cy + sin(p.angleRad) * distanza

                        // Raggio che si riduce man mano che la particella vola via
                        val raggio = boxSizePx * 0.045f * p.size * (1f - fase * 0.6f)

                        val colore = COLORI_SCINTILLE[p.colorIdx].copy(alpha = alpha)
                        drawCircle(color = colore, radius = raggio, center = Offset(px, py))

                        // Piccola coda luminosa
                        if (fase > 0.05f) {
                            val distPrev  = (fase - 0.05f) * boxSizePx * 0.9f * p.speed
                            val pxPrev    = cx + cos(p.angleRad) * distPrev
                            val pyPrev    = cy + sin(p.angleRad) * distPrev
                            drawLine(
                                color       = colore.copy(alpha = alpha * 0.4f),
                                start       = Offset(pxPrev, pyPrev),
                                end         = Offset(px, py),
                                strokeWidth = raggio * 0.8f
                            )
                        }
                    }
                }
            }
        }

        // Etichetta giorni
        Text(
            "$streakCorrente ${if (streakCorrente == 1) "giorno" else "giorni"}",
            fontSize   = 11.sp,
            color      = if (fill > 0.05f) MaterialTheme.colorScheme.tertiary
            else Color(0xFF888888),
            fontWeight = FontWeight.SemiBold
        )
    }
}