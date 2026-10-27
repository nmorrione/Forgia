package io.github.nmorrione.forgia.logic

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object It {
    val weekdays = listOf("lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica")
    val weekdayInitials = listOf("L", "M", "M", "G", "V", "S", "D")
    val monthsShort = listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic")
    val monthsFull = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
    )

    /** 1 = lunedì … 7 = domenica. */
    fun weekday(n: Int): String = weekdays[(n - 1).coerceIn(0, 6)]

    fun dayTitle(d: LocalDate) = "${weekday(d.dayOfWeek.value)} ${d.dayOfMonth} ${monthsFull[d.monthValue - 1]}"
    fun shortDate(d: LocalDate) = "${d.dayOfMonth} ${monthsShort[d.monthValue - 1]}"
    fun longDate(d: LocalDate) = "${d.dayOfMonth} ${monthsFull[d.monthValue - 1]} ${d.year}"

    fun range(a: LocalDate, b: LocalDate): String =
        if (a.month == b.month) "${a.dayOfMonth}–${b.dayOfMonth} ${monthsShort[b.monthValue - 1]}"
        else "${shortDate(a)} – ${shortDate(b)}"

    /** "Lunedì e giovedì", "Martedì, giovedì e sabato". */
    fun joinAnd(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " e " + items.last()
    }
}

/** Numero con la virgola, senza decimali inutili: 70 → "70", 62.5 → "62,5". */
fun fmtNum(v: Double, maxDecimals: Int = 2): String {
    val r = Math.round(v * 100.0) / 100.0
    if (abs(r - r.roundToInt()) < 1e-9) return r.roundToInt().toString()
    val s = String.format(Locale.US, "%.${maxDecimals}f", r).trimEnd('0').trimEnd('.')
    return s.replace('.', ',')
}

fun fmt1(v: Double): String = String.format(Locale.ITALY, "%.1f", v)

/** 2850 → "2.850". */
fun fmtInt(v: Int): String = String.format(Locale.ITALY, "%,d", v)

fun signed1(v: Double): String = (if (v >= 0) "+" else "−") + fmt1(abs(v))

/** +0,15 / −0,4: segno sempre visibile, al massimo due decimali. */
fun signed2(v: Double): String = (if (v >= 0) "+" else "−") + fmtNum(abs(v))

fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

fun capitalize(s: String) = s.replaceFirstChar { it.uppercase() }
