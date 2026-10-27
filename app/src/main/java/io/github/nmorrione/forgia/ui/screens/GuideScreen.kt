package io.github.nmorrione.forgia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.data.AppData
import io.github.nmorrione.forgia.data.Defaults
import io.github.nmorrione.forgia.data.Store
import io.github.nmorrione.forgia.data.TrainingLog
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.SectionKind
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.Note
import io.github.nmorrione.forgia.ui.components.RichText
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun GuideScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<AppData?>(null) }
    var armReset by remember { mutableStateOf(false) }
    var armDefaults by remember { mutableStateOf(false) }
    LaunchedEffect(armReset) { if (armReset) { delay(4000); armReset = false } }
    LaunchedEffect(armDefaults) { if (armDefaults) { delay(4000); armDefaults = false } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            message = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.encode(vm.data).toByteArray()) }
            }.fold({ "Backup salvato." }, { "Non sono riuscito a salvare il backup." })
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }.getOrNull()
            val parsed = text?.let { runCatching { Store.decode(it) }.getOrNull() }
            if (parsed != null) pending = parsed else message = "Il file non è un backup di Forgia."
        }
    }

    ScreenColumn {
        vm.data.guide.forEach { sec ->
            H2(sec.title)
            CardBox(padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) { RichText(sec.body) }
        }
        LinkBtn("Modifica i testi della guida", Modifier.padding(top = 6.dp)) { vm.push(Route.SectionsEdit(SectionKind.GUIDE)) }

        H2("I tuoi dati")
        CardBox {
            Text(
                "Tutto quello che segni si salva da solo sul telefono, nello spazio privato dell’app. Esporta un backup ogni tanto: è un file JSON che puoi reimportare anche su un altro telefono.",
                style = Small,
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Esporta backup", Modifier.weight(1f)) { exportLauncher.launch("forgia-backup-${LocalDate.now()}.json") }
                Btn("Importa backup", Modifier.weight(1f)) { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            }
            message?.let { Muted(it, Modifier.padding(top = 8.dp)) }
            Btn(
                if (armReset) "Tocca di nuovo per cancellare il diario" else "Cancella tutti i dati segnati",
                Modifier.padding(top = 14.dp).fillMaxWidth(), color = Pal.bad,
            ) {
                if (!armReset) armReset = true
                else {
                    vm.update { it.copy(log = TrainingLog()) }
                    armReset = false
                    message = "Diario cancellato."
                }
            }
            Btn(
                if (armDefaults) "Tocca di nuovo per ripristinare" else "Ripristina la scheda di default",
                Modifier.padding(top = 8.dp).fillMaxWidth(), color = Pal.bad,
            ) {
                if (!armDefaults) armDefaults = true
                else {
                    vm.update {
                        // Resta la data di inizio scelta; diario e timer non si toccano.
                        val start = runCatching { LocalDate.parse(it.program.startDate) }.getOrElse { LocalDate.now() }
                        Defaults.appData(start).copy(log = it.log, timer = it.timer)
                    }
                    armDefaults = false
                    message = "Programma, alimentazione e testi tornati quelli di partenza."
                }
            }
            Muted(
                "Il ripristino riporta programma, alimentazione e testi a quelli iniziali e tiene la data di inizio; diario (pesi, carichi, note) e timer restano.",
                Modifier.padding(top = 8.dp), Tiny,
            )
        }
        val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
        Note("Forgia $version")
    }

    pending?.let { data ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Importare il backup?") },
            text = { Text("Sostituisce tutti i dati attuali (programma, alimentazione, testi e diario) con quelli del file.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.replaceAll(data)
                    pending = null
                    message = "Backup importato."
                }) { Text("Importa") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Annulla") } },
        )
    }
}
