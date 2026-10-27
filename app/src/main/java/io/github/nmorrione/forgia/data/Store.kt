package io.github.nmorrione.forgia.data

import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Salva tutto in un unico file JSON nella memoria privata dell'app. */
class Store(private val file: File) {

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
            prettyPrint = true
        }

        /** Legge un file di qualsiasi versione precedente e lo porta al formato attuale. */
        fun decode(text: String): AppData = migrate(json.decodeFromString(AppData.serializer(), text))
        fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

        /**
         * I campi nuovi prendono il loro valore predefinito già in lettura;
         * qui vanno solo le trasformazioni dei dati esistenti, una versione alla volta.
         */
        fun migrate(d: AppData): AppData {
            var r = d
            // 1 → 2: aggiunte le impostazioni del timer, nessun dato da trasformare.
            if (r.schema < 2) r = r.copy(schema = 2)
            // 2 → 3: obiettivo del ciclo (Massa) e archivio dei cicli vuoto, nessun dato da trasformare.
            if (r.schema < 3) r = r.copy(schema = 3)
            return r.copy(schema = maxOf(r.schema, SCHEMA_VERSION))
        }
    }

    fun load(): AppData {
        // Primo avvio: il programma parte oggi.
        if (!file.exists()) return Defaults.appData(LocalDate.now())
        return try {
            val text = file.readText()
            val raw = json.decodeFromString(AppData.serializer(), text)
            if (raw.schema < SCHEMA_VERSION) {
                // Prima di aggiornare il formato teniamo una copia del file com'era.
                File(file.parentFile, "${file.name}.v${raw.schema}.bak").writeText(text)
            }
            migrate(raw)
        } catch (e: Exception) {
            // Non sovrascrivere un file illeggibile: lo mettiamo da parte e ripartiamo dai default.
            file.renameTo(File(file.parentFile, "${file.name}.corrotto-${System.currentTimeMillis()}"))
            Defaults.appData(LocalDate.now())
        }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(encode(data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
