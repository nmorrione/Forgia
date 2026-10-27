package io.github.nmorrione.forgia.ui.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.data.TextSection
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.SectionKind
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.theme.Pal

@Composable
fun SectionsEditor(vm: AppViewModel, kind: SectionKind) {
    val d = vm.data
    val list = when (kind) {
        SectionKind.GUIDE -> d.guide
        SectionKind.PROGRAM_INFO -> d.program.info
        SectionKind.NUTRITION -> d.nutrition.sections
    }
    val p = Pal

    fun set(newList: List<TextSection>) = vm.update { x ->
        when (kind) {
            SectionKind.GUIDE -> x.copy(guide = newList)
            SectionKind.PROGRAM_INFO -> x.copy(program = x.program.copy(info = newList))
            SectionKind.NUTRITION -> x.copy(nutrition = x.nutrition.copy(sections = newList))
        }
    }

    EditorScaffold(kind.title, vm::pop) {
        Muted("Le righe che iniziano con «- » diventano punti elenco; **testo** va in grassetto.")
        list.forEachIndexed { i, sec ->
            key(i) {
                CardBox(Modifier.padding(top = 10.dp)) {
                    TextIn("Titolo", sec.title, { v -> set(list.replaceAt(i, sec.copy(title = v))) })
                    TextIn("Testo", sec.body, { v -> set(list.replaceAt(i, sec.copy(body = v))) }, singleLine = false, minLines = 4)
                    if (kind == SectionKind.PROGRAM_INFO) {
                        SwitchRow("Aperta all’avvio", sec.open) { v -> set(list.replaceAt(i, sec.copy(open = v))) }
                    }
                    Row {
                        if (i > 0) LinkBtn("↑ Su") { set(list.move(i, -1)) }
                        if (i < list.lastIndex) LinkBtn("↓ Giù") { set(list.move(i, 1)) }
                        LinkBtn("Elimina", color = p.bad) { set(list.without(i)) }
                    }
                }
            }
        }
        LinkBtn("+ Nuova sezione", Modifier.padding(top = 8.dp)) { set(list + TextSection("Nuova sezione", "")) }
    }
}
