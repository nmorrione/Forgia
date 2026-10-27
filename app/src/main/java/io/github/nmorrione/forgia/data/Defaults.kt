package io.github.nmorrione.forgia.data

/**
 * Il programma "Massa pulita – 8 settimane" (Diario_8W_Claude.html) come dati di partenza.
 * Tutto è modificabile dall'app; "Ripristina la scheda di default" torna qui.
 */
object Defaults {

    fun appData(): AppData = AppData(
        program = program(),
        nutrition = nutrition(),
        guide = guide(),
    )

    private fun p(sets: Int, min: Int, max: Int, suffix: String = "") = Prescription(sets, min, max, suffix)

    private fun ex(
        id: String, name: String, b1: Prescription, b2: Prescription, rest: String,
        increment: String, cue: String,
    ) = Exercise(id = id, name = name, cue = cue, rest = rest, increment = increment, blocks = listOf(b1, b2))

    private const val BAR = "+2,5 kg"
    private const val BAR_LEGS = "+2,5–5 kg"
    private const val DB = "il manubrio successivo (circa +1–2 kg per lato)"
    private const val CHAIN = "+1,25–2,5 kg di zavorra sulla catena"

    fun program() = Program(
        name = "Massa pulita",
        startDate = "2026-11-23",
        weeks = 8,
        weeklyTemplate = listOf("pk", "ua", "rest", "pk", "ub", "lg", "rest"),
        blocks = listOf("Blocco 1", "Blocco 2"),
        weekPlan = listOf(
            WeekPhase(0, "3", note = "Trova i carichi giusti e cura la tecnica."),
            WeekPhase(0, "2", note = "Inizia la doppia progressione."),
            WeekPhase(0, "2", note = "Aggiungi carico dove hai chiuso il range."),
            WeekPhase(0, "1", note = "La settimana più dura del blocco."),
            WeekPhase(0, "4", deload = true, note = "Metà serie, carichi −10%, RIR 4. Coincide con Natale."),
            WeekPhase(1, "2", note = "Meno ripetizioni, più carico, una serie in più sui fondamentali."),
            WeekPhase(1, "1–2", note = "Spingi i carichi."),
            WeekPhase(1, "1", note = "Chiudi con i record della scheda."),
        ),
        deload = DeloadRule(setsFactor = 0.5, loadChangePct = -10, rir = "4"),
        warmup = "Riscaldamento: 5 minuti di mobilità, poi sul primo esercizio 50% × 8, 70% × 4, 85% × 2. Segna carico e ripetizioni di ogni serie.",
        habits = listOf(
            Habit("prot", "Proteine ≥ 150 g"),
            Habit("creat", "Creatina presa"),
            Habit("water", "3 litri d’acqua"),
        ),
        measureWeekday = 7,
        sessions = listOf(
            SessionType(
                id = "pk", name = "Parkour", short = "PK", color = 3,
                description = "Tecnica, salti e potenziamento a corpo libero.",
                tipsTitle = "Note per il parkour",
                tips = listOf(
                    Tip("Tecnica e salti all’inizio, quando sei fresco. Polsi e caviglie ben scaldati."),
                    Tip("Nel potenziamento lascia 1–2 ripetizioni in riserva su trazioni e dip: domani c’è {domani}."),
                    Tip("Se le gambe sono ancora indolenzite dallo squat di sabato, riduci gli atterraggi ad alto impatto.", weekday = 1),
                    Tip("Mangia lo spuntino pre-allenamento 1,5–2 ore prima."),
                ),
                planNotes = listOf(
                    "Le due sedute di parkour restano come le fai già: sono il tuo lavoro di potenza, salti e abilità.",
                    "Nel potenziamento a corpo libero non andare a cedimento su trazioni, dip e push-up: il giorno dopo c’è sempre una sessione di upper.",
                    "Core e mobilità possono stare qui: hollow body, L-sit, ponte, mobilità di polsi e caviglie.",
                    "Regola fissa: mai gambe con i pesi il giorno prima del parkour.",
                ),
            ),
            SessionType(
                id = "ua", name = "Upper A", short = "A", color = 0,
                description = "Forza: spinta e tirata pesanti.",
                duration = "Circa 60–75 minuti.",
                exercises = listOf(
                    ex("ua1", "Panca piana con bilanciere", p(4, 5, 7), p(5, 4, 6), "3 min", BAR,
                        "Scapole addotte, piedi ben piantati, bilanciere al petto basso. Lavora nel rack con i fermi di sicurezza."),
                    ex("ua2", "Trazioni zavorrate prone", p(4, 5, 8), p(5, 4, 6), "2–3 min", CHAIN,
                        "Catena alla cintura. Mento sopra la sbarra, discesa controllata fino a braccia distese. Se il PK di lunedì ti ha svuotato la presa, togli zavorra, non serie."),
                    ex("ua3", "Military press in piedi", p(3, 6, 8), p(4, 5, 7), "2–3 min", BAR,
                        "Glutei e addome contratti, niente schiena inarcata. Parti dai ganci del rack."),
                    ex("ua4", "Rematore con bilanciere", p(3, 8, 10), p(3, 6, 8), "2 min", BAR,
                        "Busto a circa 45°, tira verso l’ombelico, nessuno slancio."),
                    ex("ua5", "Dip zavorrate", p(3, 8, 12), p(3, 6, 10), "2 min", CHAIN,
                        "Busto leggermente in avanti, scendi finché la spalla arriva poco sotto il gomito."),
                    ex("ua6", "Curl con bilanciere", p(3, 8, 12), p(3, 8, 12), "90 s", "+1–2,5 kg",
                        "Gomiti fermi ai fianchi, discesa in 2–3 secondi."),
                    ex("ua7", "Alzate laterali con manubri", p(3, 12, 20), p(4, 12, 20), "60–90 s", DB,
                        "Busto appena inclinato in avanti, gomiti morbidi, sali fino all’altezza delle spalle."),
                ),
            ),
            SessionType(
                id = "ub", name = "Upper B", short = "B", color = 1,
                description = "Volume: petto alto, schiena, spalle e braccia.",
                duration = "Circa 60–75 minuti.",
                exercises = listOf(
                    Exercise(
                        id = "ub1", name = "Panca inclinata con manubri", rest = "2 min",
                        increment = "$DB; con il bilanciere $BAR",
                        cue = "Panca a circa 30°, discesa profonda, in alto i manubri si avvicinano senza sbattere.",
                        blocks = listOf(
                            p(4, 8, 12),
                            Prescription(4, 6, 10, variant = "Panca inclinata con bilanciere",
                                variantCue = "Stessa inclinazione (30°), presa poco più larga delle spalle."),
                        ),
                    ),
                    ex("ub2", "Chin-up zavorrati (presa supina)", p(3, 6, 10), p(4, 6, 8), "2 min", CHAIN,
                        "Presa supina alla larghezza delle spalle: lavora molto anche i bicipiti."),
                    ex("ub3", "Rematore con manubrio a un braccio", p(3, 10, 12, "per lato"), p(3, 10, 12, "per lato"), "90 s", DB,
                        "Mano e ginocchio sulla panca, porta il gomito verso l’anca, 1 secondo di pausa in alto."),
                    ex("ub4", "Shoulder press con manubri, seduto", p(3, 8, 12), p(3, 8, 12), "2 min", DB,
                        "Schienale quasi verticale, scendi fino all’altezza del mento."),
                    ex("ub5", "Alzate laterali con manubri", p(4, 12, 20), p(4, 12, 20), "60–90 s", DB,
                        "Seconda dose della settimana: i deltoidi laterali sono ciò che allarga le spalle."),
                    ex("ub6", "Alzate posteriori a busto flesso", p(3, 15, 20), p(3, 15, 20), "60 s", DB,
                        "Petto appoggiato alla panca inclinata, apri le braccia guidando con i gomiti. Carichi leggeri."),
                    ex("ub7", "Estensioni tricipiti sopra la testa", p(3, 10, 15), p(3, 10, 15), "superserie", DB,
                        "Un manubrio a due mani, seduto, allungamento completo dietro la testa. In superserie con il prossimo esercizio."),
                    ex("ub8", "Curl su panca inclinata con manubri", p(3, 10, 15), p(3, 10, 15), "90 s dopo la coppia", DB,
                        "Panca a 45–60°, braccia che pendono dietro al busto: massimo allungamento del bicipite."),
                ),
            ),
            SessionType(
                id = "lg", name = "Gambe", short = "G", color = 2,
                description = "Squat, catena posteriore, polpacci e core.",
                duration = "Circa 60–75 minuti.",
                exercises = listOf(
                    ex("lg1", "Squat con bilanciere", p(4, 5, 8), p(5, 4, 6), "3 min", BAR_LEGS,
                        "Bilanciere alto sui trapezi, fermi di sicurezza alla giusta altezza, scendi almeno al parallelo. Lunedì c’è PK: resta sempre ad almeno 1–2 ripetizioni dal cedimento."),
                    ex("lg2", "Stacco rumeno", p(3, 8, 10), p(3, 6, 8), "2–3 min", BAR_LEGS,
                        "Ginocchia leggermente flesse e ferme, anche indietro, il bilanciere sfiora le cosce. Scendi finché la schiena resta neutra."),
                    ex("lg3", "Affondi bulgari con manubri", p(3, 8, 12, "per gamba"), p(3, 8, 12, "per gamba"), "90 s", DB,
                        "Piede dietro sulla panca. Busto inclinato in avanti lavora di più i glutei, busto dritto i quadricipiti."),
                    ex("lg4", "Hip thrust con bilanciere", p(3, 8, 12), p(4, 8, 10), "2 min", BAR_LEGS,
                        "Scapole sulla panca, tatami o asciugamano sotto il bilanciere, 1 secondo di pausa in alto con il mento verso il petto."),
                    ex("lg5", "Nordic curl", p(3, 4, 8), p(3, 4, 8), "2 min", "più escursione, meno spinta con le mani",
                        "Ginocchia sul tatami, talloni bloccati sotto il bilanciere carico sul rack basso. Scendi lentamente quanto riesci, spingi con le mani per risalire. Protegge i femorali nei salti."),
                    ex("lg6", "Calf in piedi a una gamba", p(4, 10, 15, "per gamba"), p(4, 10, 15, "per gamba"), "60 s", DB,
                        "Avampiede su un disco, manubrio nella mano dello stesso lato, 1 secondo di pausa in basso in allungamento."),
                    ex("lg7", "Leg raise alla sbarra", p(3, 8, 15), p(3, 8, 15), "60–90 s", "gambe tese, poi un manubrio leggero tra i piedi",
                        "Arrotola il bacino verso l’alto, niente oscillazioni. Quando diventa facile, gambe tese o un manubrio leggero tra i piedi."),
                ),
            ),
            SessionType(
                id = "rest", name = "Riposo", short = "–", color = 4, rest = true,
                description = "Recupero, un po’ di movimento e meal prep.",
                tipsTitle = "Cosa fare oggi",
                tips = listOf(
                    Tip("Meal prep per lunedì, martedì e mercoledì.", weekday = 7),
                    Tip("Meal prep per giovedì, venerdì e sabato.", weekday = 3),
                    Tip("Se riesci, 20–30 minuti di camminata e 10 minuti di mobilità per anche, caviglie e torace."),
                    Tip("Misura la vita al mattino (campo qui sotto). Ogni 2 settimane una foto dell’addome, stessa luce.", weekday = 7),
                    Tip("Calorie da giorno di riposo: metà carboidrati a cena, niente shake post-allenamento."),
                ),
            ),
        ),
        info = listOf(
            TextSection(
                "RIR: quanto spingere",
                """
                RIR significa ripetizioni in riserva: quante ripetizioni pulite potresti ancora fare a fine serie. Con RIR 2 ti fermi quando ne avresti ancora 2.
                Ogni settimana ha il suo RIR, scritto sotto la data nella vista Oggi. Vale per l’ultima serie di ogni esercizio; le prime possono essere un filo più facili.
                """.trimIndent(),
            ),
            TextSection(
                "Doppia progressione",
                """
                Ogni esercizio ha un intervallo di ripetizioni, per esempio 5–7. Tieni lo stesso carico finché completi il massimo dell’intervallo in tutte le serie rispettando il RIR. A quel punto aumenti il carico e riparti dal minimo.
                - Panca, military, rematore: +2,5 kg
                - Squat, stacco rumeno, hip thrust: +2,5–5 kg
                - Manubri: il disco successivo (circa +1–2 kg per lato)
                - Zavorra su trazioni e dip: +1,25–2,5 kg sulla catena
                Nella vista Oggi trovi sotto ogni esercizio cosa hai fatto l’ultima volta e se è il momento di aumentare.
                """.trimIndent(),
            ),
            TextSection(
                "Riscaldamento e recuperi",
                """
                5 minuti di mobilità di spalle, anche e polsi. Sul primo esercizio multiarticolare: 50% × 8, 70% × 4, 85% × 2, poi le serie di lavoro. Negli esercizi successivi basta una serie leggera.
                Rispetta i recuperi indicati: sui fondamentali 2–3 minuti servono davvero per spostare più carico.
                """.trimIndent(),
            ),
            TextSection(
                "Perché è costruita così",
                """
                Il parkour del lunedì e del giovedì è già molto lavoro di gambe esplosivo e di tirata a corpo libero, quindi i pesi coprono quello che il corpo libero fa meno bene: carichi pesanti e progressivi, deltoidi, braccia e catena posteriore.
                Ogni muscolo della parte alta viene allenato due volte con i pesi, più il parkour. Le gambe una volta pesante, con nordic curl e calf per proteggere femorali e tendini nei salti.
                """.trimIndent(),
            ),
        ),
    )

    /* ---------- Alimentazione ---------- */

    private fun food(
        id: String, name: String, kcal: Double, p: Double, c: Double, f: Double, role: FoodRole,
        unit: String = "g", piece: String? = null, plural: String? = null, pieceGrams: Double? = null,
        subs: Boolean = true,
    ) = Food(id, name, kcal, p, c, f, role, unit, piece, plural, pieceGrams, subs)

    fun foods() = listOf(
        // Proteine
        food("pollo", "Petto di pollo o tacchino (crudo)", 100.0, 23.0, 0.0, 1.2, FoodRole.PROTEIN),
        food("manzo", "Manzo magro 5% (crudo)", 130.0, 21.0, 0.0, 5.0, FoodRole.PROTEIN),
        food("merluzzo", "Merluzzo o nasello", 72.0, 17.0, 0.0, 0.7, FoodRole.PROTEIN),
        food("salmone", "Salmone o sgombro", 205.0, 20.0, 0.0, 13.5, FoodRole.PROTEIN),
        food("tonno", "Tonno al naturale, sgocciolato", 103.0, 25.0, 0.0, 0.5, FoodRole.PROTEIN),
        food("uova", "Uova intere", 128.0, 12.4, 0.0, 8.7, FoodRole.PROTEIN, piece = "uovo", plural = "uova", pieceGrams = 60.0),
        food("albume", "Albume", 43.0, 10.7, 0.7, 0.0, FoodRole.PROTEIN),
        food("yogurt", "Yogurt greco 0–2%", 63.0, 10.0, 3.5, 1.0, FoodRole.PROTEIN),
        food("fiocchilatte", "Fiocchi di latte", 85.0, 12.0, 2.0, 3.0, FoodRole.PROTEIN),
        food("whey", "Proteine del siero", 390.0, 82.0, 6.0, 5.0, FoodRole.PROTEIN),
        // Carboidrati
        food("riso", "Riso basmati o pasta integrale (crudi)", 350.0, 8.0, 75.0, 1.5, FoodRole.CARBS),
        food("farro", "Farro o orzo (crudi)", 335.0, 13.0, 67.0, 2.5, FoodRole.CARBS),
        food("avena", "Fiocchi d’avena", 372.0, 13.0, 60.0, 7.0, FoodRole.CARBS),
        food("pane", "Pane integrale", 240.0, 9.0, 45.0, 2.0, FoodRole.CARBS),
        food("patate", "Patate", 77.0, 2.0, 17.0, 0.1, FoodRole.CARBS),
        food("gallette", "Gallette di riso", 380.0, 8.0, 80.0, 2.8, FoodRole.CARBS),
        food("gnocchi", "Gnocchi di patate", 150.0, 4.0, 32.0, 0.6, FoodRole.CARBS),
        food("miele", "Miele o marmellata", 304.0, 0.3, 82.0, 0.0, FoodRole.CARBS, subs = false),
        // Grassi
        food("olio", "Olio extravergine", 899.0, 0.0, 0.0, 100.0, FoodRole.FAT),
        food("fruttasecca", "Mandorle o noci", 600.0, 21.0, 9.0, 52.0, FoodRole.FAT),
        food("arachidi", "Burro d’arachidi 100%", 600.0, 25.0, 12.0, 50.0, FoodRole.FAT),
        food("avocado", "Avocado", 160.0, 2.0, 2.0, 15.0, FoodRole.FAT),
        // Fissi
        food("latte", "Latte parzialmente scremato o bevanda di soia", 46.0, 3.3, 4.9, 1.6, FoodRole.FIXED, unit = "ml"),
        food("banana", "Banana", 89.0, 1.1, 23.0, 0.3, FoodRole.FIXED, piece = "banana", plural = "banane", pieceGrams = 120.0),
        food("mela", "Mela", 52.0, 0.3, 14.0, 0.2, FoodRole.FIXED, piece = "mela", plural = "mele", pieceGrams = 150.0),
        food("verdure", "Verdure", 25.0, 1.5, 4.0, 0.2, FoodRole.FIXED),
    )

    private fun i(food: String, grams: Double) = MealItem(food, grams)

    private fun breakfast(id: String) = Meal(
        id, "7:30", "Colazione",
        listOf(i("avena", 80.0), i("latte", 250.0), i("banana", 120.0), i("arachidi", 15.0)),
        alternatives = listOf(
            "100 g pane integrale tostato, 2 uova strapazzate con 100 g di albume, 2 kiwi",
            "250 g yogurt greco, 60 g avena o muesli senza zucchero, frutti di bosco, 15 g di noci",
        ),
    )

    private fun officeSnack(id: String) = Meal(
        id, "10:30", "Spuntino in ufficio",
        listOf(i("yogurt", 170.0), i("fruttasecca", 20.0), i("mela", 150.0)),
        alternatives = listOf(
            "30 g di proteine in polvere nello shaker con acqua, 20 g di mandorle, 1 frutto (non serve il frigo)",
            "150 g fiocchi di latte, 2 gallette di riso, 1 frutto",
        ),
    )

    private fun lunch(id: String) = Meal(
        id, "13:00", "Pranzo preparato",
        listOf(i("riso", 110.0), i("pollo", 150.0), i("verdure", 200.0), i("olio", 15.0)),
        note = "Almeno 200 g di verdure.",
        alternativesTitle = "Rotazione per la settimana",
        alternatives = listOf(
            "Riso basmati, pollo al curry (spezie e yogurt, non panna), zucchine",
            "Pasta integrale al ragù di manzo magro 5% fatto in casa, insalata",
            "Farro con pollo, 100 g di ceci, pomodorini e rucola",
            "450 g patate al forno, tacchino alla piastra, broccoli",
            "Riso venere con salmone o gamberi e verdure saltate",
        ),
    )

    private fun bedtime(id: String) = Meal(
        id, "23:00", "Prima di dormire, facoltativo",
        listOf(i("yogurt", 200.0)),
        note = "Solo se a fine giornata sei sotto l’obiettivo di proteine. Va bene anche con i fiocchi di latte.",
        optional = true,
        scalable = false,
    )

    fun trainMeals() = listOf(
        breakfast("t-colazione"),
        officeSnack("t-spuntino"),
        lunch("t-pranzo"),
        Meal(
            "t-pre", "16:30", "Pre-allenamento",
            listOf(i("pane", 80.0), i("miele", 20.0), i("banana", 120.0)),
            note = "Soprattutto carboidrati: energia per la seduta.",
            alternatives = listOf(
                "50 g gallette di riso con 20 g di miele e 1 frutto",
                "150 g yogurt greco con 40 g di avena e miele (se ti alleni almeno 2 ore dopo)",
            ),
        ),
        Meal(
            "t-post", "19:45", "Dopo l’allenamento",
            listOf(i("whey", 30.0)),
            note = "In acqua o latte. Se ceni entro un’ora puoi saltarlo e cenare direttamente.",
            scalable = false,
        ),
        Meal(
            "t-cena", "20:30", "Cena",
            listOf(i("salmone", 150.0), i("patate", 350.0), i("verdure", 200.0), i("olio", 10.0)),
            note = "Salmone o un’altra proteina dalla tabella; verdure a volontà.",
            alternatives = listOf(
                "180 g manzo magro, 100 g di pasta o riso (crudo), verdure",
                "2 uova e 150 g di albume in frittata con verdure, 120 g di pane integrale",
                "200 g merluzzo, 100 g di riso, verdure, 15 g di olio",
                "Pasta e legumi: 80 g di pasta, 150 g di legumi lessati, 30 g di parmigiano",
            ),
        ),
        bedtime("t-notte"),
    )

    fun restMeals() = listOf(
        breakfast("r-colazione"),
        officeSnack("r-spuntino"),
        lunch("r-pranzo"),
        Meal(
            "r-pomeriggio", "16:30", "Spuntino del pomeriggio",
            listOf(i("pane", 80.0), i("tonno", 80.0), i("verdure", 100.0)),
            note = "Il tonno si può sostituire con 2 uova sode; verdura cruda a piacere.",
            alternatives = listOf(
                "150 g fiocchi di latte con 2 gallette e pomodorini",
                "50 g bresaola con pane integrale (al massimo una volta a settimana)",
            ),
        ),
        Meal(
            "r-cena", "20:30", "Cena",
            listOf(i("salmone", 150.0), i("patate", 175.0), i("verdure", 200.0), i("olio", 10.0)),
            note = "Salmone o un’altra proteina dalla tabella; verdure a volontà.",
            alternatives = listOf(
                "180 g manzo magro, 50 g di pasta o riso (crudo), verdure",
                "2 uova e 150 g di albume in frittata con verdure, 60 g di pane integrale",
                "200 g merluzzo, 50 g di riso, verdure, 15 g di olio",
                "Pasta e legumi: 50 g di pasta, 150 g di legumi lessati, 30 g di parmigiano",
            ),
        ),
        bedtime("r-notte"),
    )

    fun nutrition() = Nutrition(
        bodyWeightKg = 70.0,
        train = DayTarget(kcal = 2850, protein = 160, fat = 80),
        rest = DayTarget(kcal = 2500, protein = 155, fat = 80),
        targetNote = "Stime di partenza per 70 kg, lavoro sedentario e 5 sedute a settimana: circa 150–200 kcal sopra il mantenimento in media settimanale. È la bilancia a dire se vanno ritoccate.",
        foods = foods(),
        trainMeals = trainMeals(),
        restMeals = restMeals(),
        substitutions = listOf(
            SubstitutionGroup(
                "Proteine: circa 30 g", FoodRole.PROTEIN, SubBasis.PROTEIN, "pollo", 130.0,
                note = "Legumi: 250 g lessati danno circa 20 g di proteine, abbinali a cereali o a un po’ di formaggio.",
            ),
            SubstitutionGroup("Carboidrati: come 80 g di riso crudo", FoodRole.CARBS, SubBasis.KCAL, "riso", 80.0),
            SubstitutionGroup("Grassi: circa 10 g", FoodRole.FAT, SubBasis.FAT, "olio", 10.0),
        ),
        sections = listOf(
            TextSection(
                "Regole di qualità",
                """
                - Pesce 3 volte a settimana, di cui 2 pesce grasso (salmone, sgombro, sardine).
                - Legumi almeno 3 volte a settimana, anche nel pranzo o come contorno.
                - Carne rossa magra al massimo 2 volte. Bresaola o crudo sgrassato al massimo una volta. Salame, mortadella e würstel restano fuori.
                - Tonno in scatola al massimo 2–3 volte a settimana, per il mercurio.
                - Verdure a pranzo e a cena, 2–3 frutti al giorno, per arrivare ad almeno 30 g di fibre.
                - Una pizza a settimana va benissimo: tieni alte le proteine nel resto della giornata.
                - Fritti, dolci confezionati e bibite zuccherate: occasionali. Alcol poco e raro, peggiora sonno e recupero.
                """.trimIndent(),
            ),
            TextSection(
                "Meal prep",
                """
                - **Domenica**: pranzi di lunedì, martedì e mercoledì. Per esempio 330 g di riso crudo, 450 g di pollo, una teglia di verdure al forno, divisi in 3 contenitori. Rassoda 4 uova e porziona yogurt e frutta secca.
                - **Mercoledì**: seconda infornata per giovedì, venerdì e sabato.
                - Pesa da crudo, poi dividi il cotto in parti uguali: non serve pesare ogni porzione.
                - Riso e carne cotti in frigo al massimo 3 giorni. Stendi il riso in un contenitore basso per raffreddarlo in fretta e mettilo in frigo entro un’ora.
                - Se in ufficio non c’è frigo, borsa termica con siberino.
                """.trimIndent(),
            ),
            TextSection(
                "Integratori",
                """
                - **Creatina monoidrato**: 3–5 g ogni giorno, anche a riposo; l’orario non conta. Nelle prime 2–3 settimane il peso sale di 0,5–1 kg per l’acqua nei muscoli: non è grasso.
                - **Proteine del siero**: un alimento comodo, non obbligatorio. Usale quando non arrivi all’obiettivo.
                - **Vitamina D e omega-3**: in inverno e con poco pesce possono servire, ma valutale con il medico, magari dopo un esame del sangue.
                """.trimIndent(),
            ),
        ),
        footer = "Indicazioni generali per una persona sana, non sostituiscono un nutrizionista o un medico.",
    )

    /* ---------- Guida ---------- */

    fun guide() = listOf(
        TextSection(
            "Misurare",
            """
            - **Peso**: ogni mattina, o almeno 4 volte a settimana, a digiuno dopo il bagno. Conta solo la media settimanale, che il calendario calcola da solo.
            - **Vita**: una volta a settimana al mattino, all’altezza dell’ombelico, pancia rilassata. È il miglior allarme per l’addome.
            - **Foto**: addome ogni 2 settimane, stessa luce e stessa ora.
            - **Ritmo giusto**: da +0,15 a +0,3 kg a settimana, quindi circa 1–2 kg in 8 settimane, di cui una parte è acqua della creatina.
            """.trimIndent(),
        ),
        TextSection(
            "Correggere le calorie: ogni 2 settimane, non prima",
            """
            - La media sale meno di 0,1 kg a settimana e la vita è ferma: **+150 kcal** al giorno, per esempio 40 g di riso crudo in più a pranzo.
            - La media sale più di 0,4 kg a settimana, oppure la vita cresce di 1 cm in 2 settimane: **−150 kcal**, per esempio 15 g di olio in meno.
            - Sei nel range e i carichi salgono: non toccare niente.
            - Carichi fermi per 2 sedute di fila su più esercizi: controlla prima sonno e cibo, poi anticipa la settimana di scarico.
            Il calendario applica queste regole da solo e ti propone la correzione: se la accetti, le quantità dei pasti si ricalcolano.
            """.trimIndent(),
        ),
        TextSection(
            "Giornata da programmatore",
            """
            - Alzati ogni 45–60 minuti per 2–3 minuti: acqua, una rampa di scale, mobilità di anche e torace. Puoi legarlo al ritmo del lavoro, per esempio a ogni commit o dopo ogni riunione.
            - Se puoi, 10 minuti di camminata dopo pranzo.
            - Bottiglia da un litro sulla scrivania, da finire tre volte.
            - Il piano tiene già conto del lavoro seduto: se un giorno cammini molto di più, puoi aggiungere un frutto o una porzione di carboidrati.
            """.trimIndent(),
        ),
        TextSection(
            "Sonno",
            """
            - 7,5–9 ore. È lì che il muscolo cresce davvero.
            - Ultimo caffè entro le 15–16.
            - Nell’ultima mezz’ora niente schermi da lavoro: è il momento giusto per lo yogurt serale e la lettura.
            """.trimIndent(),
        ),
        TextSection(
            "Parkour e pesi insieme",
            """
            - Lunedì PK, martedì Upper A, giovedì PK, venerdì Upper B: nel potenziamento del parkour niente cedimento su trazioni e dip.
            - Sabato Gambe, domenica riposo, lunedì PK: le gambe hanno 48 ore prima dei salti. Per questo lo squat resta sempre a 1–2 ripetizioni dal cedimento.
            - Se devi spostare una seduta, l’unica regola fissa è: mai gambe il giorno prima del parkour. Dalla vista Oggi puoi cambiare la sessione di un giorno.
            """.trimIndent(),
        ),
        TextSection(
            "Le feste",
            """
            - La settimana 5 (21–27 dicembre) è di scarico e cade a Natale: se salti una seduta non succede niente.
            - Il 25 dicembre e il 1° gennaio sono venerdì, giorno di Upper B: sposta la seduta o saltala.
            - Pranzi e cenoni: goditeli, tieni alte le proteine negli altri pasti e non compensare digiunando il giorno dopo.
            - Dopo una cena abbondante il peso può salire di 1–2 kg in un giorno: è acqua e sale. Guarda la media della settimana dopo.
            """.trimIndent(),
        ),
    )
}
