package io.github.nmorrione.forgia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Meal
import io.github.nmorrione.forgia.data.Nutrition
import io.github.nmorrione.forgia.logic.NutritionCalc
import io.github.nmorrione.forgia.logic.ScaleResult
import io.github.nmorrione.forgia.logic.fmtInt
import io.github.nmorrione.forgia.logic.fmtNum
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.SectionKind
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.Bullet
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.ExpandCard
import io.github.nmorrione.forgia.ui.components.H2
import io.github.nmorrione.forgia.ui.components.H3
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.components.Note
import io.github.nmorrione.forgia.ui.components.RichText
import io.github.nmorrione.forgia.ui.components.ScreenColumn
import io.github.nmorrione.forgia.ui.edit.newId
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodScreen(vm: AppViewModel) {
    val n = vm.data.nutrition
    val day = vm.foodDay
    val tr = day == DayType.TRAIN
    val target = n.target(day)
    val meals = n.meals(day)
    val tot = NutritionCalc.dayMacros(meals, n)
    var preview by remember { mutableStateOf<ScaleResult?>(null) }
    val p = Pal

    ScreenColumn {
        Row(Modifier.padding(top = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Allenamento", Modifier.weight(1f), filled = tr) { vm.foodDay = DayType.TRAIN }
            Btn("Riposo", Modifier.weight(1f), filled = !tr) { vm.foodDay = DayType.REST }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroBox(fmtInt(target.kcal), "kcal", Modifier.weight(1f))
            MacroBox("${target.protein} g", "proteine", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroBox("${target.carbs} g", "carboidrati", Modifier.weight(1f))
            MacroBox("${target.fat} g", "grassi", Modifier.weight(1f))
        }
        CardBox(Modifier.padding(top = 8.dp)) {
            Text("La giornata tipo qui sotto: ${NutritionCalc.macroLine(tot)}", style = Small)
            val diff = (tot.kcal - target.kcal).roundToInt()
            Muted(
                if (abs(diff) <= 50) "In linea con l’obiettivo."
                else "${if (diff > 0) "+" else "−"}${fmtInt(abs(diff))} kcal rispetto all’obiettivo: puoi adattare le quantità.",
                Modifier.padding(top = 4.dp), Tiny,
            )
            FlowRow {
                LinkBtn("Adatta le quantità") { preview = NutritionCalc.scaleMeals(meals, n, target) }
                LinkBtn("Modifica obiettivi") { vm.push(Route.TargetsEdit) }
                LinkBtn("Alimenti") { vm.push(Route.Foods) }
            }
        }
        if (n.targetNote.isNotBlank()) Note(n.targetNote)

        H2("La giornata tipo")
        meals.forEach { m -> MealRow(m, n) { vm.push(Route.MealEdit(day, m.id)) } }
        LinkBtn("+ Aggiungi un pasto") {
            val id = newId("m")
            vm.updateNutrition { it.withMeals(day, it.meals(day) + Meal(id, "12:00", "Nuovo pasto")) }
            vm.push(Route.MealEdit(day, id))
        }

        H2("Sostituzioni")
        n.substitutions.forEachIndexed { gi, g ->
            ExpandCard(g.title, initiallyOpen = gi == 0) {
                Row(Modifier.padding(bottom = 6.dp)) {
                    Text("Alimento", style = cond(16), color = p.muted, modifier = Modifier.weight(1f))
                    Text("Quantità", style = cond(16), color = p.muted)
                }
                HorizontalDivider(color = p.line)
                NutritionCalc.substitutionRows(g, n).forEach { (food, grams) ->
                    Row(Modifier.padding(vertical = 8.dp)) {
                        Text(food.name, style = Body.copy(fontSize = 15.sp), modifier = Modifier.weight(1f).padding(end = 10.dp))
                        Text(
                            NutritionCalc.itemText(io.github.nmorrione.forgia.data.MealItem(food.id, grams), food)
                                .let { txt -> if (food.pieceName != null) "$txt (${fmtNum(grams)} g)" else "${fmtNum(grams)} ${food.unit}" },
                            style = Body.copy(fontSize = 15.sp), textAlign = TextAlign.End,
                        )
                    }
                    HorizontalDivider(color = p.line)
                }
                if (g.note.isNotBlank()) Note(g.note)
            }
        }
        LinkBtn("Modifica le tabelle") { vm.push(Route.SubstitutionsEdit) }

        n.sections.forEach { sec ->
            H2(sec.title)
            CardBox(padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) { RichText(sec.body) }
        }
        LinkBtn("Modifica i testi", Modifier.padding(top = 6.dp)) { vm.push(Route.SectionsEdit(SectionKind.NUTRITION)) }
        if (n.footer.isNotBlank()) Note(n.footer)
    }

    preview?.let { r ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Adatta le quantità") },
            text = {
                Column {
                    Text("Obiettivo: ${fmtInt(target.kcal)} kcal, P ${target.protein} g, C ${target.carbs} g, G ${target.fat} g", style = Small)
                    Text("Prima: ${NutritionCalc.macroLine(r.before)}", style = Small, modifier = Modifier.padding(top = 8.dp))
                    Text("Dopo: ${NutritionCalc.macroLine(r.after)}", style = Small.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Cambiano i grammi delle fonti di proteine, carboidrati e grassi; frutta, verdura e i pasti fissi restano uguali.",
                        style = Tiny, color = p.muted, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateNutrition { it.withMeals(day, r.meals) }
                    preview = null
                }) { Text("Applica") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun MacroBox(value: String, label: String, modifier: Modifier) {
    CardBox(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = cond(34, FontWeight.Bold, 1.0))
        Text(label, style = Tiny, color = Pal.muted)
    }
}

@Composable
private fun MealRow(m: Meal, n: Nutrition, onEdit: () -> Unit) {
    val p = Pal
    var open by rememberSaveable(m.id) { mutableStateOf(false) }
    Row(Modifier.padding(bottom = 10.dp)) {
        Text(
            m.time, style = cond(19, FontWeight.Bold), color = p.muted, textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp).padding(top = 14.dp),
        )
        Spacer(Modifier.width(10.dp))
        CardBox(Modifier.weight(1f), padding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)) {
            Row {
                H3(m.name, Modifier.weight(1f))
                Text("Modifica", style = Tiny.copy(fontWeight = FontWeight.SemiBold), color = p.accent, modifier = Modifier.clickable(onClick = onEdit).padding(4.dp))
            }
            Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                m.items.forEach { Bullet(NutritionCalc.itemText(it, n.food(it.foodId)), Body.copy(fontSize = 15.sp, lineHeight = 21.sp)) }
            }
            val mm = NutritionCalc.mealMacros(m, n)
            Muted(
                "≈ ${fmtInt(mm.kcal.roundToInt())} kcal, ${mm.protein.roundToInt()} g di proteine" + if (m.optional) " · facoltativo" else "",
                style = Tiny,
            )
            if (m.note.isNotBlank()) Muted(m.note, Modifier.padding(top = 2.dp), Tiny)
            if (m.alternatives.any { it.isNotBlank() }) {
                Text(
                    m.alternativesTitle.ifBlank { "Alternative" },
                    style = Small.copy(fontWeight = FontWeight.SemiBold), color = p.accent,
                    modifier = Modifier.padding(top = 6.dp).clickable { open = !open }.padding(vertical = 4.dp),
                )
                if (open) {
                    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        m.alternatives.filter { it.isNotBlank() }.forEach { Bullet(it, Small.copy(fontSize = 15.sp)) }
                    }
                }
            }
        }
    }
}
