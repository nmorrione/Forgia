package io.github.nmorrione.forgia.data

import kotlinx.serialization.json.Json
import java.io.File

/** Salva tutto in un unico file JSON nella memoria privata dell'app. */
class Store(private val file: File) {

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
            prettyPrint = true
        }

        fun decode(text: String): AppData = json.decodeFromString(AppData.serializer(), text)
        fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)
    }

    fun load(): AppData {
        if (!file.exists()) return Defaults.appData()
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            // Non sovrascrivere un file illeggibile: lo mettiamo da parte e ripartiamo dai default.
            file.renameTo(File(file.parentFile, "${file.name}.corrotto-${System.currentTimeMillis()}"))
            Defaults.appData()
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
