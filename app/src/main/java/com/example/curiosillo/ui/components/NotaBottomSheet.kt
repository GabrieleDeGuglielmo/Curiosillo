package com.example.curiosillo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.curiosillo.ui.theme.Primary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotaBottomSheet(
    notaAttuale: String,
    onSalva:     (String) -> Unit,
    onChiudi:    () -> Unit
) {
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope        = rememberCoroutineScope()
    var testo        by remember { mutableStateOf(notaAttuale) }
    val focusRequester = remember { FocusRequester() }

    val animateChiudi: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onChiudi()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onChiudi,
        sheetState       = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "📝 La mia nota",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Scrivi un appunto personale su questa pillola",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = testo,
                onValueChange = { testo = it },
                placeholder   = { Text("Es. Da approfondire, collegato a...") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .focusRequester(focusRequester),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 6
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = animateChiudi,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text("Annulla") }

                Button(
                    onClick  = { onSalva(testo); animateChiudi() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salva")
                }
            }
        }

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}
