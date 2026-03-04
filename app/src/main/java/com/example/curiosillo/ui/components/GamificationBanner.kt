package com.example.curiosillo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curiosillo.domain.LivelloHelper
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Tertiary

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
                        color      = Secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        livello.titolo,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 28.sp)
                    Text(
                        "$streakCorrente ${if (streakCorrente == 1) "giorno" else "giorni"}",
                        fontSize   = 11.sp,
                        color      = Tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$xpTotali XP", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                if (xpMancanti > 0)
                    Text(
                        "$xpMancanti XP al prossimo livello",
                        fontSize = 11.sp,
                        color    = Color(0xFFAAAAAA)
                    )
                else
                    Text("Livello massimo!", fontSize = 11.sp, color = Tertiary)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { progressione },
                modifier   = Modifier.fillMaxWidth().height(8.dp),
                color      = Primary,
                trackColor = Color(0xFF333355)
            )
        }
    }
}
