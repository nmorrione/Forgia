# Forgia

App Android per seguire un programma di allenamento a blocchi e il piano alimentare che lo accompagna. È nata da un diario HTML di 8 settimane ([Diario_8W_Claude.html](Diario_8W_Claude.html)), che resta nel repository come riferimento: il suo contenuto è il programma di default.

Tutto è modificabile dall'app, e i valori derivati si ricalcolano da soli.

## Cosa fa

**Oggi**: la sessione del giorno con la prescrizione della settimana (blocco, RIR, scarico). Per ogni esercizio segni carico e ripetizioni serie per serie, vedi cosa hai fatto l'ultima volta e se è il momento di aumentare (doppia progressione). Ci sono poi l'esito del cibo, le abitudini da spuntare, il peso, la vita nel giorno della misura e le note.

**Calendario**: la griglia del programma, le statistiche, il grafico del peso con la media settimanale e il riepilogo di ogni settimana. Applica le regole della guida (ogni 2 settimane, ±150 kcal) e propone la correzione: se la accetti, obiettivi e pasti si ricalcolano.

**Schede**: le sessioni con le prescrizioni per blocco, le fasi settimana per settimana e i testi su RIR, progressione e riscaldamento.

**Cibo**: gli obiettivi per i giorni di allenamento e di riposo, con i carboidrati calcolati dalle calorie. La giornata tipo è composta da alimenti con i valori per 100 g, quindi kcal e macro di ogni pasto sono calcolati. Ci sono anche le tabelle delle sostituzioni, calcolate, e il pulsante **Adatta le quantità**, che ricalcola i grammi delle fonti di proteine, carboidrati e grassi per centrare l'obiettivo.

**Guida**: i testi del diario, backup ed esportazione in JSON, ripristino della scheda di default.

**Programma** (in alto a destra): data di inizio, durata, settimana tipo, sessioni ed esercizi, blocchi, fasi, regola di scarico e abitudini.

Il diario è legato alle date del calendario, non al programma: se sposti l'inizio o ricominci un nuovo ciclo, carichi e pesi restano. Lo storico «ultima volta» continua anche tra un programma e l'altro.

## Compilare

Servono il JDK 17 o superiore (va bene quello incluso in Android Studio) e l'Android SDK con la piattaforma 35.

```bash
./gradlew assembleRelease
```

L'APK esce in `app/build/outputs/apk/release/app-release.apk`. Su Windows, se `JAVA_HOME` non è impostato, si può usare il JDK di Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleRelease
```

### Firma

La build di release usa `keystore.properties` nella cartella principale, se c'è:

```properties
storeFile=forgia-release.jks
storePassword=...
keyAlias=forgia
keyPassword=...
```

Il file e il keystore sono esclusi da git. **Tienine una copia di sicurezza**: per installare un aggiornamento sopra l'app esistente serve la stessa firma. Senza `keystore.properties`, la release viene firmata con la chiave di debug.

## Test

```bash
./gradlew testDebugUnitTest
```

I test controllano che il programma di default corrisponda all'HTML: date, fasi, scarico, totali dei pasti e sostituzioni. Verificano anche il ricalcolo delle quantità, i suggerimenti di progressione e calorie, e il salvataggio in JSON.

## Struttura

- `data/`: modello dei dati (`Model.kt`), programma di default (`Defaults.kt`), salvataggio JSON (`Store.kt`)
- `logic/`: calendario e prescrizioni (`Schedule.kt`), diario e suggerimenti (`Tracker.kt`), calcoli nutrizionali e ricalcolo delle quantità (`NutritionCalc.kt`)
- `ui/`: Jetpack Compose, con le cinque sezioni in `screens/` e gli editor in `edit/`

Kotlin, Jetpack Compose e Material 3, kotlinx.serialization. Nessun permesso richiesto, nessun dato esce dal telefono.

---

Indicazioni generali per una persona sana: non sostituiscono un nutrizionista o un medico.
