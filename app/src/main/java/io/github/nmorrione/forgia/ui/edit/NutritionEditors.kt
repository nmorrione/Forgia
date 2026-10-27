package io.github.nmorrione.forgia.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nmorrione.forgia.data.DayTarget
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Food
import io.github.nmorrione.forgia.data.FoodRole
import io.github.nmorrione.forgia.data.Meal
import io.github.nmorrione.forgia.data.MealItem
import io.github.nmorrione.forgia.data.SubBasis
import io.github.nmorrione.forgia.data.SubstitutionGroup
import io.github.nmorrione.forgia.logic.It
import io.github.nmorrione.forgia.logic.NutritionCalc
import io.github.nmorrione.forgia.logic.NutritionCalc.macros
import io.github.nmorrione.forgia.logic.fmtNum
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.Route
import io.github.nmorrione.forgia.ui.components.Btn
import io.github.nmorrione.forgia.ui.components.CardBox
import io.github.nmorrione.forgia.ui.components.LinkBtn
import io.github.nmorrione.forgia.ui.components.Muted
import io.github.nmorrione.forgia.ui.theme.Body
import io.github.nmorrione.forgia.ui.theme.Pal
import io.github.nmorrione.forgia.ui.theme.Small
import io.github.nmorrione.forgia.ui.theme.Tiny
import io.github.nmorrione.forgia.ui.theme.cond
import java.time.LocalDate
import kotlin.math.roundToInt

private fun dayName(day: DayType) = if (day == DayType.TRAIN) "Giorno di allenamento" else "Giorno di riposo"

/* ---------- Obiettivi ---------- */

@Composable
fun TargetsEditor(vm: AppViewModel) {
    val n = vm.data.nutrition
    var message by remember { mutableStateOf<String?>(null) }
    val bw = n.bodyWeightKg.coerceAtLeast(1.0)

    EditorScaffold("Obiettivi", vm::pop) {
        NumIn("Peso corporeo (kg)", n.bodyWeightKg, { v -> vm.updateNutrition { it.copy(bodyWeightKg = v) } }, key = "bw")
        DayType.entries.forEach { day ->
            val t = n.target(day)
            fun set(nt: DayTarget) = vm.updateNutrition { it.withTarget(day, nt) }
            EditorSection(dayName(day))
            CardBox {
                IntIn("Calorie (kcal)", t.kcal, { set(t.copy(kcal = it)) }, key = "$day/kcal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntIn("Proteine (g)", t.protein, { set(t.copy(protein = it)) }, key = "$day/p", modifier = Modifier.weight(1f))
                    IntIn("Grassi (g)", t.fat, { set(t.copy(fat = it)) }, key = "$day/f", modifier = Modifier.weight(1f))
                }
                Muted(
                    "Carboidrati: ${t.carbs} g, calcolati dalle calorie che restano. Proteine ${fmtNum(t.protein / bw, 1)} g/kg, grassi ${fmtNum(t.fat / bw, 1)} g/kg.",
                    Modifier.padding(top = 8.dp), Tiny,
                )
                Muted("Pasti attuali: ${NutritionCalc.macroLine(NutritionCalc.dayMacros(n.meals(day), n))}", Modifier.padding(top = 4.dp), Tiny)
            }
        }
        Btn("Adatta le quantità dei pasti agli obiettivi", Modifier.padding(top = 16.dp).fillMaxWidth(), filled = true) {
            vm.updateNutrition { NutritionCalc.rescaleDay(NutritionCalc.rescaleDay(it, DayType.TRAIN), DayType.REST) }
            message = "Quantità ricalcolate per entrambi i tipi di giornata."
        }
        message?.let { Muted(it, Modifier.padding(top = 8.dp)) }
        Muted(
            "Il ricalcolo cambia i grammi delle fonti di proteine, carboidrati e grassi; frutta, verdura e i pasti fissi restano uguali.",
            Modifier.padding(top = 8.dp), Tiny,
        )
        TextIn("Nota sotto gli obiettivi", n.targetNote, { v -> vm.updateNutrition { it.copy(targetNote = v) } }, singleLine = false)

        EditorSection("Correzione automatica")
        Muted("Ogni tot settimane il calendario confronta la media del peso e la vita e propone di aggiungere o togliere calorie.", style = Tiny)
        val a = n.adjust
        fun setA(f: (io.github.nmorrione.forgia.data.AdjustRule) -> io.github.nmorrione.forgia.data.AdjustRule) =
            vm.updateNutrition { it.copy(adjust = f(it.adjust)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn("Aumento minimo (kg/sett.)", a.minWeeklyGain, { v -> setA { it.copy(minWeeklyGain = v) } }, key = "a-min", modifier = Modifier.weight(1f))
            NumIn("Aumento massimo (kg/sett.)", a.maxWeeklyGain, { v -> setA { it.copy(maxWeeklyGain = v) } }, key = "a-max", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn("Allarme vita (cm)", a.waistAlarmCm, { v -> setA { it.copy(waistAlarmCm = v) } }, key = "a-waist", modifier = Modifier.weight(1f))
            IntIn("Correzione (kcal)", a.stepKcal, { v -> setA { it.copy(stepKcal = v) } }, key = "a-step", modifier = Modifier.weight(1f))
        }
        IntIn("Ogni quante settimane", a.everyWeeks, { v -> setA { it.copy(everyWeeks = v.coerceIn(1, 8)) } }, key = "a-every")

        if (n.adjustments.isNotEmpty()) {
            EditorSection("Correzioni applicate")
            n.adjustments.forEach { adj ->
                Text(
                    "${It.shortDate(LocalDate.parse(adj.date))}: ${if (adj.deltaKcal >= 0) "+" else "−"}${kotlin.math.abs(adj.deltaKcal)} kcal",
                    style = Body,
                )
            }
        }
    }
}

/* ---------- Pasto ---------- */

@Composable
fun MealEditor(vm: AppViewModel, day: DayType, mealId: String) {
    val n = vm.data.nutrition
    val meal = n.meals(day).firstOrNull { it.id == mealId } ?: return MissingItem(vm)
    val index = n.meals(day).indexOfFirst { it.id == mealId }
    val other = if (day == DayType.TRAIN) DayType.REST else DayType.TRAIN
    val p = Pal
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val foodOptions = remember(n.foods) {
        n.foods.sortedWith(compareBy({ it.role.ordinal }, { it.name })).map { it.id to it.name }
    }

    fun upd(f: (Meal) -> Meal) = vm.updateNutrition { it.withMeals(day, it.meals(day).map { m -> if (m.id == mealId) f(m) else m }) }

    EditorScaffold(meal.name.ifBlank { "Pasto" }, vm::pop) {
        Muted(dayName(day))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextIn("Ora", meal.time, { v -> upd { it.copy(time = v) } }, Modifier.width(96.dp))
            TextIn("Nome", meal.name, { v -> upd { it.copy(name = v) } }, Modifier.weight(1f))
        }

        EditorSection("Alimenti")
        meal.items.forEachIndexed { i, item ->
            val food = n.food(item.foodId)
            key(i) {
                CardBox(Modifier.padding(bottom = 8.dp)) {
                    Picker("Alimento", foodOptions, item.foodId, { fid -> upd { it.copy(items = it.items.replaceAt(i, item.copy(foodId = fid))) } })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumIn(
                            "Quantità (${food?.unit ?: "g"})", item.grams,
                            { v -> upd { it.copy(items = it.items.replaceAt(i, item.copy(grams = v))) } },
                            key = "$mealId/$i", modifier = Modifier.weight(1f),
                        )
                        LinkBtn("Elimina", color = p.bad) { upd { it.copy(items = it.items.without(i)) } }
                    }
                    val m = NutritionCalc.itemMacros(item, n)
                    Muted(
                        "${NutritionCalc.itemText(item, food)} · ${m.kcal.roundToInt()} kcal · P ${fmtNum(m.protein, 1)} · C ${fmtNum(m.carbs, 1)} · G ${fmtNum(m.fat, 1)}",
                        Modifier.padding(top = 4.dp), Tiny,
                    )
                }
            }
        }
        Row {
            LinkBtn("+ Aggiungi alimento") {
                upd { it.copy(items = it.items + MealItem(n.foods.firstOrNull()?.id ?: "", 100.0)) }
            }
            LinkBtn("Gestisci alimenti") { vm.push(Route.Foods) }
        }
        CardBox(Modifier.padding(top = 8.dp)) {
            Text("Totale: ${NutritionCalc.macroLine(NutritionCalc.mealMacros(meal, n))}", style = Small.copy(fontWeight = FontWeight.SemiBold))
        }

        EditorSection("Opzioni")
        TextIn("Nota", meal.note, { v -> upd { it.copy(note = v) } }, singleLine = false)
        SwitchRow("Facoltativo", meal.optional, help = "Non conta nei totali della giornata.") { v -> upd { it.copy(optional = v) } }
        SwitchRow("Ricalcolabile", meal.scalable, help = "«Adatta le quantità» può cambiare i grammi di questo pasto.") { v -> upd { it.copy(scalable = v) } }

        EditorSection("Alternative")
        TextIn("Titolo", meal.alternativesTitle, { v -> upd { it.copy(alternativesTitle = v) } })
        TextIn(
            "Un’alternativa per riga", meal.alternatives.joinToString("\n"), { v -> upd { it.copy(alternatives = v.lines()) } },
            singleLine = false, minLines = 3,
        )

        EditorSection("Ordine e copia")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Sposta su", Modifier.weight(1f), enabled = index > 0) {
                vm.updateNutrition { it.withMeals(day, it.meals(day).move(index, -1)) }
            }
            Btn("Sposta giù", Modifier.weight(1f), enabled = index < n.meals(day).lastIndex) {
                vm.updateNutrition { it.withMeals(day, it.meals(day).move(index, 1)) }
            }
        }
        Btn("Copia nel ${dayName(other).replaceFirstChar { it.lowercase() }}", Modifier.padding(top = 8.dp).fillMaxWidth()) {
            vm.updateNutrition { it.withMeals(other, it.meals(other) + meal.copy(id = newId("m"))) }
            message = "Copiato: lo trovi in fondo alla giornata."
        }
        message?.let { Muted(it, Modifier.padding(top = 8.dp)) }
        Btn("Elimina il pasto", Modifier.padding(top = 8.dp).fillMaxWidth(), color = p.bad) { confirmDelete = true }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Eliminare «${meal.name}»?", "Il pasto sparisce solo dal ${dayName(day).replaceFirstChar { it.lowercase() }}.", "Elimina",
            onConfirm = {
                vm.pop()
                vm.updateNutrition { it.withMeals(day, it.meals(day).filter { m -> m.id != mealId }) }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ---------- Alimenti ---------- */

@Composable
fun FoodsEditor(vm: AppViewModel) {
    val n = vm.data.nutrition
    EditorScaffold("Alimenti", vm::pop) {
        Muted("Valori per 100 g (o 100 ml). Il ruolo decide come l’alimento viene ricalcolato e in quale tabella delle sostituzioni compare.")
        FoodRole.entries.forEach { role ->
            val list = n.foods.filter { it.role == role }.sortedBy { it.name }
            if (list.isNotEmpty()) {
                EditorSection(NutritionCalc.roleName(role))
                list.forEach { f ->
                    key(f.id) {
                        CardBox(Modifier.padding(bottom = 8.dp), onClick = { vm.push(Route.FoodEdit(f.id)) }) {
                            Text(f.name, style = cond(18))
                            Muted("${fmtNum(f.kcal)} kcal · P ${fmtNum(f.protein)} · C ${fmtNum(f.carbs)} · G ${fmtNum(f.fat)}", style = Tiny)
                        }
                    }
                }
            }
        }
        Btn("+ Nuovo alimento", Modifier.padding(top = 16.dp).fillMaxWidth()) {
            val id = newId("f")
            vm.updateNutrition { it.copy(foods = it.foods + Food(id, "Nuovo alimento", 100.0, 0.0, 0.0, 0.0)) }
            vm.push(Route.FoodEdit(id))
        }
    }
}

@Composable
fun FoodEditor(vm: AppViewModel, id: String) {
    val n = vm.data.nutrition
    val f = n.food(id) ?: return MissingItem(vm)
    val p = Pal
    val used = (n.trainMeals + n.restMeals).any { m -> m.items.any { it.foodId == id } } || n.substitutions.any { it.refFoodId == id }

    fun upd(g: (Food) -> Food) = vm.updateNutrition { it.copy(foods = it.foods.map { x -> if (x.id == id) g(x) else x }) }

    EditorScaffold(f.name.ifBlank { "Alimento" }, vm::pop) {
        TextIn("Nome", f.name, { v -> upd { it.copy(name = v) } })
        Picker("Ruolo", FoodRole.entries.map { it to NutritionCalc.roleName(it) }, f.role, { r -> upd { it.copy(role = r) } })
        Picker("Unità", listOf("g" to "grammi", "ml" to "millilitri"), f.unit, { u -> upd { it.copy(unit = u) } })

        EditorSection("Valori per 100 ${f.unit}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn("kcal", f.kcal, { v -> upd { it.copy(kcal = v) } }, key = "$id/kcal", modifier = Modifier.weight(1f))
            NumIn("Proteine (g)", f.protein, { v -> upd { it.copy(protein = v) } }, key = "$id/p", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn("Carboidrati (g)", f.carbs, { v -> upd { it.copy(carbs = v) } }, key = "$id/c", modifier = Modifier.weight(1f))
            NumIn("Grassi (g)", f.fat, { v -> upd { it.copy(fat = v) } }, key = "$id/f", modifier = Modifier.weight(1f))
        }
        val calc = f.protein * 4 + f.carbs * 4 + f.fat * 9
        Row(verticalAlignment = Alignment.CenterVertically) {
            Muted("Dai macronutrienti: ${calc.roundToInt()} kcal", Modifier.weight(1f), Tiny)
            LinkBtn("Usa questo valore") { upd { it.copy(kcal = calc.roundToInt().toDouble()) } }
        }

        EditorSection("Pezzi (facoltativo)")
        Muted("Se imposti un pezzo, nei pasti vedrai «1 banana» invece dei grammi.", style = Tiny)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextIn("Singolare", f.pieceName ?: "", { v -> upd { it.copy(pieceName = v.ifBlank { null }) } }, Modifier.weight(1f))
            TextIn("Plurale", f.piecePlural ?: "", { v -> upd { it.copy(piecePlural = v.ifBlank { null }) } }, Modifier.weight(1f))
        }
        NumIn("Peso di un pezzo (g)", f.pieceGrams ?: 0.0, { v -> upd { it.copy(pieceGrams = v.takeIf { g -> g > 0 }) } }, key = "$id/piece")
        SwitchRow("Compare nelle sostituzioni", f.inSubstitutions) { v -> upd { it.copy(inSubstitutions = v) } }

        EditorSection("Elimina")
        if (used) {
            Muted("È usato in un pasto o come riferimento di una tabella: per eliminarlo, prima sostituiscilo lì.")
        } else {
            Btn("Elimina l’alimento", color = p.bad) {
                vm.pop()
                vm.updateNutrition { it.copy(foods = it.foods.filter { x -> x.id != id }) }
            }
        }
    }
}

/* ---------- Sostituzioni ---------- */

@Composable
fun SubstitutionsEditor(vm: AppViewModel) {
    val n = vm.data.nutrition
    val p = Pal
    val foodOptions = remember(n.foods) { n.foods.sortedWith(compareBy({ it.role.ordinal }, { it.name })).map { it.id to it.name } }
    val bases = listOf(SubBasis.PROTEIN to "proteine", SubBasis.CARBS to "carboidrati", SubBasis.FAT to "grassi", SubBasis.KCAL to "calorie")

    fun upd(i: Int, f: (SubstitutionGroup) -> SubstitutionGroup) =
        vm.updateNutrition { it.copy(substitutions = it.substitutions.replaceAt(i, f(it.substitutions[i]))) }

    EditorScaffold("Sostituzioni", vm::pop) {
        Muted("Ogni tabella elenca gli alimenti di un ruolo nella quantità che dà lo stesso valore del riferimento. Le quantità si ricalcolano da sole.")
        n.substitutions.forEachIndexed { i, g ->
            key(i) {
                CardBox(Modifier.padding(top = 10.dp)) {
                    TextIn("Titolo", g.title, { v -> upd(i) { it.copy(title = v) } })
                    Picker("Alimenti da elencare", FoodRole.entries.map { it to NutritionCalc.roleName(it) }, g.role, { r -> upd(i) { it.copy(role = r) } })
                    Picker("Equivalenza per", bases, g.basis, { b -> upd(i) { it.copy(basis = b) } })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Picker("Riferimento", foodOptions, g.refFoodId, { fid -> upd(i) { it.copy(refFoodId = fid) } }, Modifier.weight(1.5f))
                        NumIn("Grammi", g.refGrams, { v -> upd(i) { it.copy(refGrams = v) } }, key = "sub$i", modifier = Modifier.weight(1f))
                    }
                    n.food(g.refFoodId)?.let { ref ->
                        val v = NutritionCalc.basisValue(ref.macros(g.refGrams), g.basis)
                        Muted("Riferimento: ${fmtNum(v, 1)} ${NutritionCalc.basisUnit(g.basis)} di ${bases.first { it.first == g.basis }.second}.", Modifier.padding(top = 6.dp), Tiny)
                    }
                    TextIn("Nota", g.note, { v -> upd(i) { it.copy(note = v) } }, singleLine = false)
                    LinkBtn("Elimina la tabella", color = p.bad) { vm.updateNutrition { it.copy(substitutions = it.substitutions.without(i)) } }
                }
            }
        }
        LinkBtn("+ Nuova tabella", Modifier.padding(top = 8.dp)) {
            vm.updateNutrition {
                it.copy(substitutions = it.substitutions + SubstitutionGroup("Nuova tabella", FoodRole.PROTEIN, SubBasis.PROTEIN, it.foods.firstOrNull()?.id ?: "", 100.0))
            }
        }
    }
}
