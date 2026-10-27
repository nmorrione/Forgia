package io.github.nmorrione.forgia.logic

import io.github.nmorrione.forgia.data.Adjustment
import io.github.nmorrione.forgia.data.DayTarget
import io.github.nmorrione.forgia.data.DayType
import io.github.nmorrione.forgia.data.Food
import io.github.nmorrione.forgia.data.FoodRole
import io.github.nmorrione.forgia.data.Meal
import io.github.nmorrione.forgia.data.MealItem
import io.github.nmorrione.forgia.data.Nutrition
import io.github.nmorrione.forgia.data.SubBasis
import io.github.nmorrione.forgia.data.SubstitutionGroup
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

data class Macros(val kcal: Double = 0.0, val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0) {
    operator fun plus(o: Macros) = Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
}

data class ScaleResult(val meals: List<Meal>, val before: Macros, val after: Macros)

object NutritionCalc {

    fun Food.macros(grams: Double): Macros {
        val f = grams / 100.0
        return Macros(kcal * f, protein * f, carbs * f, fat * f)
    }

    fun itemMacros(item: MealItem, n: Nutrition): Macros = n.food(item.foodId)?.macros(item.grams) ?: Macros()

    fun mealMacros(meal: Meal, n: Nutrition): Macros =
        meal.items.fold(Macros()) { acc, it -> acc + itemMacros(it, n) }

    /** Totale della giornata, senza i pasti facoltativi. */
    fun dayMacros(meals: List<Meal>, n: Nutrition): Macros =
        meals.filter { !it.optional }.fold(Macros()) { acc, m -> acc + mealMacros(m, n) }

    /** "80 g fiocchi d’avena", "1 banana", "250 ml latte…". */
    fun itemText(item: MealItem, food: Food?): String {
        if (food == null) return "${fmtNum(item.grams)} g (alimento eliminato)"
        val pg = food.pieceGrams
        if (food.pieceName != null && pg != null && pg > 0) {
            val pieces = item.grams / pg
            val half = (pieces * 2).roundToInt() / 2.0
            if (half > 0 && abs(pieces - half) < 0.08) {
                val name = if (half > 1) food.piecePlural ?: food.pieceName else food.pieceName
                return "${fmtNum(half)} $name"
            }
        }
        val name = food.name.replaceFirstChar { it.lowercase() }
        return "${fmtNum(item.grams)} ${food.unit} $name"
    }

    /** "2.850 kcal, P 160 g, C 372 g, G 80 g". */
    fun macroLine(m: Macros): String =
        "${fmtInt(m.kcal.roundToInt())} kcal, P ${m.protein.roundToInt()} g, C ${m.carbs.roundToInt()} g, G ${m.fat.roundToInt()} g"

    fun roleName(role: FoodRole): String = when (role) {
        FoodRole.PROTEIN -> "Fonte di proteine"
        FoodRole.CARBS -> "Fonte di carboidrati"
        FoodRole.FAT -> "Fonte di grassi"
        FoodRole.FIXED -> "Fisso (frutta, verdura, latte…)"
    }

    fun basisValue(m: Macros, basis: SubBasis): Double = when (basis) {
        SubBasis.PROTEIN -> m.protein
        SubBasis.CARBS -> m.carbs
        SubBasis.FAT -> m.fat
        SubBasis.KCAL -> m.kcal
    }

    fun basisUnit(basis: SubBasis): String = when (basis) {
        SubBasis.KCAL -> "kcal"
        else -> "g"
    }

    /** Righe della tabella: quanti grammi di ogni alimento equivalgono al riferimento. */
    fun substitutionRows(g: SubstitutionGroup, n: Nutrition): List<Pair<Food, Double>> {
        val ref = n.food(g.refFoodId) ?: return emptyList()
        val amount = basisValue(ref.macros(g.refGrams), g.basis)
        if (amount <= 0) return emptyList()
        return n.foods
            .filter { it.role == g.role && (it.inSubstitutions || it.id == ref.id) }
            .mapNotNull { f ->
                val per100 = basisValue(f.macros(100.0), g.basis)
                if (per100 <= 0) null else f to roundStep(amount / per100 * 100.0, 5.0)
            }
    }

    fun roundStep(v: Double, step: Double): Double = (v / step).roundToInt() * step

    /**
     * Ricalcola i grammi dei pasti per avvicinarsi all'obiettivo.
     * Gli alimenti fonte di proteine, carboidrati e grassi hanno ciascuno un fattore di scala;
     * i tre fattori si trovano con i minimi quadrati sui grammi di P, C e G (pesati in kcal).
     * Frutta, verdura e i pasti non ricalcolabili restano fissi.
     */
    fun scaleMeals(meals: List<Meal>, n: Nutrition, target: DayTarget): ScaleResult {
        val roles = listOf(FoodRole.PROTEIN, FoodRole.CARBS, FoodRole.FAT)
        val a = Array(3) { DoubleArray(3) }
        val fixed = DoubleArray(3)
        for (meal in meals) {
            if (meal.optional) continue
            for (item in meal.items) {
                val food = n.food(item.foodId) ?: continue
                val m = food.macros(item.grams)
                val v = doubleArrayOf(m.protein, m.carbs, m.fat)
                val col = roles.indexOf(food.role)
                if (meal.scalable && col >= 0) for (k in 0..2) a[k][col] += v[k]
                else for (k in 0..2) fixed[k] += v[k]
            }
        }
        val goal = doubleArrayOf(target.protein.toDouble(), target.carbs.toDouble(), target.fat.toDouble())
        val weights = doubleArrayOf(4.0, 4.0, 9.0)
        val active = (0..2).filter { c -> (0..2).any { a[it][c] > 1e-6 } }
        val factors = DoubleArray(3) { 1.0 }
        if (active.isNotEmpty()) {
            val sol = leastSquares(active.size, { k, j -> weights[k] * a[k][active[j]] }, { k -> weights[k] * (goal[k] - fixed[k]) })
            active.forEachIndexed { j, c -> factors[c] = sol[j].coerceIn(0.25, 4.0) }
        }
        val scaled = meals.map { meal ->
            if (meal.optional || !meal.scalable) meal
            else meal.copy(items = meal.items.map { item ->
                val col = n.food(item.foodId)?.let { roles.indexOf(it.role) } ?: -1
                if (col < 0) item else {
                    val g = item.grams * factors[col]
                    item.copy(grams = roundStep(g, if (g < 30) 1.0 else 5.0).coerceAtLeast(1.0))
                }
            })
        }
        return ScaleResult(scaled, dayMacros(meals, n), dayMacros(scaled, n))
    }

    /** Minimi quadrati su 3 equazioni con un piccolo richiamo verso 1 (nessun cambiamento). */
    private fun leastSquares(n: Int, a: (Int, Int) -> Double, b: (Int) -> Double): DoubleArray {
        val m = Array(n) { DoubleArray(n + 1) }
        var trace = 0.0
        for (i in 0 until n) for (j in 0 until n) {
            var s = 0.0
            for (k in 0..2) s += a(k, i) * a(k, j)
            m[i][j] = s
            if (i == j) trace += s
        }
        val lambda = 1e-4 * (trace / n).coerceAtLeast(1e-9)
        for (i in 0 until n) {
            var s = 0.0
            for (k in 0..2) s += a(k, i) * b(k)
            m[i][i] += lambda
            m[i][n] = s + lambda
        }
        for (col in 0 until n) {
            val piv = (col until n).maxBy { abs(m[it][col]) }
            val tmp = m[col]; m[col] = m[piv]; m[piv] = tmp
            if (abs(m[col][col]) < 1e-12) continue
            for (r in 0 until n) if (r != col) {
                val f = m[r][col] / m[col][col]
                for (c in col..n) m[r][c] -= f * m[col][c]
            }
        }
        return DoubleArray(n) { i -> if (abs(m[i][i]) < 1e-12) 1.0 else m[i][n] / m[i][i] }
    }

    fun rescaleDay(n: Nutrition, day: DayType): Nutrition =
        n.withMeals(day, scaleMeals(n.meals(day), n, n.target(day)).meals)

    /** Applica una correzione delle calorie a entrambi i tipi di giornata e ricalcola i pasti. */
    fun applyCalorieChange(n: Nutrition, delta: Int, today: LocalDate): Nutrition {
        var r = n.copy(
            train = n.train.copy(kcal = n.train.kcal + delta),
            rest = n.rest.copy(kcal = n.rest.kcal + delta),
            adjustments = n.adjustments + Adjustment(today.toString(), delta),
        )
        r = rescaleDay(r, DayType.TRAIN)
        r = rescaleDay(r, DayType.REST)
        return r
    }
}
