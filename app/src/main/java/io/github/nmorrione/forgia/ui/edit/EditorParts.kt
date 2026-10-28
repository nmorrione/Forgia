package io.github.nmorrione.forgia.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.logic.fmtNum
import io.github.nmorrione.forgia.timer.fmtClock
import io.github.nmorrione.forgia.logic.parseNum
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.RoundIcon
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import kotlin.math.roundToInt

fun newId(prefix: String): String = prefix + System.currentTimeMillis().toString(36)

fun <T> List<T>.replaceAt(i: Int, v: T): List<T> = toMutableList().also { it[i] = v }
fun <T> List<T>.without(i: Int): List<T> = toMutableList().also { it.removeAt(i) }
fun <T> List<T>.move(i: Int, delta: Int): List<T> {
    val j = i + delta
    if (j !in indices) return this
    return toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
}

@Composable
fun EditorScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon("‹", size = 40.dp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(title, style = cond(24), maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
        ScreenColumn(Modifier.weight(1f)) { content() }
    }
}

@Composable
fun MissingItem(vm: AppViewModel) {
    EditorScaffold("Non trovato", vm::pop) {
        Muted("Questo elemento non esiste più.", Modifier.padding(top = 16.dp), Body)
    }
}

@Composable
fun EditorSection(title: String) = H2(title, top = 24.dp)

@Composable
fun TextIn(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    help: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.padding(top = 6.dp),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        supportingText = help?.let { { Text(it) } },
    )
}

/** Campo numerico con etichetta; tiene il testo mentre scrivi e aggiorna il valore quando è valido. */
@Composable
fun NumIn(
    label: String,
    value: Double,
    onChange: (Double) -> Unit,
    key: Any,
    modifier: Modifier = Modifier.fillMaxWidth(),
    decimals: Boolean = true,
    signed: Boolean = false,
    help: String? = null,
) {
    var text by remember(key) { mutableStateOf(fmtNum(value)) }
    LaunchedEffect(key, value) {
        if (parseNum(text) != value) text = fmtNum(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val clean = raw.filterIndexed { i, ch ->
                ch.isDigit() || (decimals && (ch == ',' || ch == '.')) || (signed && ch == '-' && i == 0)
            }.take(9)
            text = clean
            parseNum(clean)?.let(onChange)
        },
        modifier = modifier.padding(top = 6.dp),
        label = { Text(label) },
        singleLine = true,
        supportingText = help?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
    )
}

@Composable
fun IntIn(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    key: Any,
    modifier: Modifier = Modifier.fillMaxWidth(),
    signed: Boolean = false,
    help: String? = null,
) = NumIn(label, value.toDouble(), { onChange(it.roundToInt()) }, key, modifier, decimals = false, signed = signed, help = help)

@Composable
fun <T> Picker(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.padding(top = 6.dp)) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "—",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("▾", color = Pal.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, l) ->
                DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); open = false })
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, help: String? = null, onChange: (Boolean) -> Unit) {
    val p = Pal
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold))
            help?.let { Text(it, style = Tiny, color = p.muted) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = p.accent),
        )
    }
}

@Composable
fun Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        RoundIcon("−", enabled = value > min, size = 38.dp) { onChange(value - 1) }
        Text("$value", style = cond(22), textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
        RoundIcon("+", enabled = value < max, size = 38.dp) { onChange(value + 1) }
    }
}

/** Come [Stepper], ma per una durata: mostra "3:00" e va a passi di [step] secondi. */
@Composable
fun TimeStepper(
    label: String,
    seconds: Int,
    step: Int = 15,
    min: Int = 0,
    max: Int = 30 * 60,
    help: String? = null,
    onChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = Body.copy(fontWeight = FontWeight.SemiBold))
            help?.let { Text(it, style = Tiny, color = Pal.muted) }
        }
        RoundIcon("−", enabled = seconds > min, size = 38.dp) { onChange((seconds - step).coerceAtLeast(min)) }
        Text(fmtClock(seconds * 1000L), style = cond(22), textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
        RoundIcon("+", enabled = seconds < max, size = 38.dp) { onChange((seconds + step).coerceAtMost(max)) }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
