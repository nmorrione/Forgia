package io.github.nmorrione.forgia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.logic.fmtNum
import io.github.nmorrione.forgia.logic.parseNum
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.cond

/** Spazio in fondo occupato dalla barra del timer, se visibile: il contenuto ci scorre sopra. */
val LocalOverlayPadding = staticCompositionLocalOf { 0.dp }

/** Colonna scorrevole larga al massimo 560dp, come il layout dell'HTML. */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp + LocalOverlayPadding.current),
            content = content,
        )
    }
}

@Composable
fun H2(text: String, modifier: Modifier = Modifier, top: Dp = 28.dp) {
    Text(text, style = cond(25), modifier = modifier.padding(top = top, bottom = 10.dp))
}

@Composable
fun H3(text: String, modifier: Modifier = Modifier) {
    Text(text, style = cond(20), modifier = modifier)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, style: TextStyle = Small) {
    Text(text, style = style, color = Pal.muted, modifier = modifier)
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = Pal.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** **grassetto** dentro una riga. */
fun rich(s: String): AnnotatedString = buildAnnotatedString {
    s.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}

@Composable
fun Bullet(text: String, style: TextStyle = Body) {
    Row(Modifier.padding(bottom = 6.dp)) {
        Text("•", style = style, modifier = Modifier.width(18.dp))
        Text(rich(text), style = style, modifier = Modifier.weight(1f))
    }
}

/** Testo semplice: le righe che iniziano con "- " sono punti elenco. */
@Composable
fun RichText(body: String, style: TextStyle = Body) {
    Column {
        body.lines().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
            if (line.startsWith("- ")) Bullet(line.removePrefix("- "), style)
            else Text(rich(line), style = style, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

@Composable
fun TipsCard(lines: List<String>) {
    CardBox(padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
        lines.forEach { Bullet(it) }
    }
}

@Composable
fun ExpandCard(
    title: String,
    initiallyOpen: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    CardBox(modifier = modifier.padding(bottom = 8.dp), padding = PaddingValues(0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = cond(20), modifier = Modifier.weight(1f))
            Text(if (open) "–" else "+", style = Body.copy(fontSize = Body.fontSize * 1.5), color = Pal.muted)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), content = content)
        }
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) { content() }
}

@Composable
fun Chip(text: String, selected: Boolean, dot: Color? = null, onClick: () -> Unit) {
    val p = Pal
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) p.btn else p.surface)
            .border(1.dp, if (selected) p.btn else p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null) Dot(dot)
        Text(text, style = Small.copy(fontWeight = FontWeight.Medium), color = if (selected) p.btnInk else p.ink)
    }
}

/** Pulsante pieno o a contorno, nello stile dei segmenti dell'HTML. */
@Composable
fun Btn(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    color: Color? = null,
    onClick: () -> Unit,
) {
    val p = Pal
    val shape = RoundedCornerShape(10.dp)
    val fg = when {
        filled -> p.btnInk
        color != null -> color
        else -> p.ink
    }
    Box(
        modifier
            .clip(shape)
            .background(if (filled) p.btn else p.surface)
            .border(1.dp, if (filled) p.btn else color ?: p.line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Body.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            color = if (enabled) fg else fg.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun LinkBtn(text: String, modifier: Modifier = Modifier, color: Color? = null, onClick: () -> Unit) {
    Text(
        text,
        style = Small.copy(fontWeight = FontWeight.SemiBold, fontSize = Small.fontSize * 1.07),
        color = color ?: Pal.accent,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
fun RoundIcon(text: String, enabled: Boolean = true, size: Dp = 44.dp, onClick: () -> Unit) {
    val p = Pal
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(p.surface)
            .border(1.dp, p.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Body.copy(fontSize = Body.fontSize * 1.4), color = if (enabled) p.ink else p.ink.copy(alpha = 0.3f))
    }
}

/** Campo compatto con sfondo, per numeri e testi brevi. */
@Composable
fun SmallField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    style: TextStyle = cond(18),
    center: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val p = Pal
    val align = if (center) TextAlign.Center else TextAlign.Start
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = style.copy(color = p.ink, textAlign = align),
        cursorBrush = SolidColor(p.accent),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(p.surface2, RoundedCornerShape(10.dp))
                    .border(1.dp, p.line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = if (center) Alignment.Center else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = p.muted.copy(alpha = 0.7f), textAlign = align), modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.fillMaxWidth()) { inner() }
            }
        },
    )
}

/** Campo numerico: tiene il testo scritto (es. "70,") e restituisce il valore letto. */
@Composable
fun NumberInput(
    value: Double?,
    onValue: (Double?) -> Unit,
    key: Any,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    decimals: Boolean = true,
    style: TextStyle = cond(18),
) {
    var text by remember(key) { mutableStateOf(value?.let { fmtNum(it) } ?: "") }
    LaunchedEffect(key, value) {
        if (parseNum(text) != value) text = value?.let { fmtNum(it) } ?: ""
    }
    SmallField(
        value = text,
        onValueChange = { raw ->
            val clean = raw.filter { it.isDigit() || (decimals && (it == ',' || it == '.')) }.take(7)
            text = clean
            onValue(parseNum(clean))
        },
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
        style = style,
    )
}

@Composable
fun Banner(text: String) {
    val p = Pal
    Box(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(p.surface, RoundedCornerShape(12.dp))
            .border(1.dp, p.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { Text(text, style = Small.copy(fontSize = Small.fontSize * 1.07)) }
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Muted(text, modifier.padding(top = 14.dp), style = Small.copy(fontSize = Small.fontSize * 0.93))
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

/** 1 = in linea, 2 = quasi, 3 = fuori. */
@Composable
fun foodColor(v: Int): Color = when (v) {
    1 -> Pal.good
    2 -> Pal.warn
    else -> Pal.bad
}
