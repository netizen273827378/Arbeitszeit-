package de.arbeitszeit.rechner

/**
 * Alle Zeiten werden als "Minuten seit Mitternacht" (0..1439) behandelt.
 * Beispiel: 7:30 Uhr = 7 * 60 + 30 = 450.
 */
data class ArbeitszeitRegeln(
    /** Tägliche Sollarbeitszeit: 7,8 Std. = 7:48 = 468 Minuten (5 × 7,8 = 39 Std./Woche). */
    val sollMinutenProTag: Int = 468,
    /** Gutschrift fürs Einstempeln am PC (Weg zum Arbeitsplatz, PC hochfahren, anmelden). */
    val gutschriftEinstempelnMinuten: Int = 15,
    /** Pauschaler Pausenabzug, unabhängig von der tatsächlichen Pausendauer. */
    val pausenAbzugMinuten: Int = 30,
    val gleitzeitBeginn: Int = 6 * 60,
    val gleitzeitEnde: Int = 20 * 60,
    val kernzeitBeginn: Int = 9 * 60,
    val kernzeitEnde: Int = 15 * 60,
    /** Gesetzliche Höchstarbeitszeit (ArbZG) als Info: 10 Std. */
    val maxArbeitMinutenProTag: Int = 10 * 60,
)

data class Ergebnis(
    /** Zeitpunkt, ab dem die Arbeitszeit gezählt wird (Einstempeln − Gutschrift, frühestens Gleitzeitbeginn). */
    val angerechneterBeginn: Int,
    /** Rein rechnerisches Ende, an dem die Sollzeit erreicht ist (ohne Kernzeit/Gleitzeit-Grenzen). */
    val sollErreichtUm: Int,
    /** Frühestmögliches Ausstempeln ohne Minusstunden (unter Beachtung von Kern- und Gleitzeit). */
    val fruehestesAusstempeln: Int,
    /** Spätestes Ausstempeln (10 Std. Arbeitszeit bzw. Gleitzeitende). */
    val spaetestesAusstempeln: Int,
    /** Saldo, wenn man genau zum frühestmöglichen Zeitpunkt ausstempelt (positiv = Plus, negativ = Minus). */
    val saldoBeiFruehestemAusstempeln: Int,
    val hinweise: List<String>,
)

class ArbeitszeitRechner(private val regeln: ArbeitszeitRegeln = ArbeitszeitRegeln()) {

    /** Zeitpunkt, ab dem gezählt wird. */
    fun angerechneterBeginn(einstempeln: Int): Int =
        maxOf(einstempeln - regeln.gutschriftEinstempelnMinuten, regeln.gleitzeitBeginn)

    /** Angerechnete Arbeitszeit (nach Pausenabzug) zwischen Ein- und Ausstempeln. */
    fun gearbeiteteMinuten(einstempeln: Int, ausstempeln: Int): Int {
        val beginn = angerechneterBeginn(einstempeln)
        val ende = minOf(ausstempeln, regeln.gleitzeitEnde)
        val anwesend = ende - beginn
        if (anwesend <= 0) return 0
        return maxOf(anwesend - regeln.pausenAbzugMinuten, 0)
    }

    /** Tagessaldo: positiv = Plusstunden, negativ = Minusstunden. */
    fun saldo(einstempeln: Int, ausstempeln: Int): Int =
        gearbeiteteMinuten(einstempeln, ausstempeln) - regeln.sollMinutenProTag

    fun berechne(einstempeln: Int): Ergebnis {
        val hinweise = mutableListOf<String>()
        val beginn = angerechneterBeginn(einstempeln)

        if (einstempeln - regeln.gutschriftEinstempelnMinuten < regeln.gleitzeitBeginn) {
            hinweise += "Zeit vor ${formatUhrzeit(regeln.gleitzeitBeginn)} Uhr (Gleitzeitbeginn) wird nicht " +
                "angerechnet – die Zählung beginnt um ${formatUhrzeit(beginn)} Uhr."
        }
        if (einstempeln > regeln.kernzeitBeginn) {
            hinweise += "Achtung: Eingestempelt nach Beginn der Kernarbeitszeit " +
                "(${formatUhrzeit(regeln.kernzeitBeginn)} Uhr)."
        }

        val sollErreicht = beginn + regeln.sollMinutenProTag + regeln.pausenAbzugMinuten
        var fruehestens = sollErreicht

        if (fruehestens < regeln.kernzeitEnde) {
            fruehestens = regeln.kernzeitEnde
            hinweise += "Wegen der Kernarbeitszeit kannst du erst um ${formatUhrzeit(regeln.kernzeitEnde)} Uhr " +
                "gehen (Sollzeit wäre schon um ${formatUhrzeit(sollErreicht)} Uhr erreicht) – " +
                "dadurch ${formatDauer(regeln.kernzeitEnde - sollErreicht)} Plusstunden."
        }
        if (fruehestens > regeln.gleitzeitEnde) {
            fruehestens = regeln.gleitzeitEnde
            hinweise += "Die Sollzeit ist vor Gleitzeitende (${formatUhrzeit(regeln.gleitzeitEnde)} Uhr) nicht " +
                "erreichbar – es entstehen ${formatDauer(sollErreicht - regeln.gleitzeitEnde)} Minusstunden."
        }

        val spaetestens = minOf(
            beginn + regeln.maxArbeitMinutenProTag + regeln.pausenAbzugMinuten,
            regeln.gleitzeitEnde,
        )

        return Ergebnis(
            angerechneterBeginn = beginn,
            sollErreichtUm = sollErreicht,
            fruehestesAusstempeln = fruehestens,
            spaetestesAusstempeln = maxOf(spaetestens, fruehestens),
            saldoBeiFruehestemAusstempeln = saldo(einstempeln, fruehestens),
            hinweise = hinweise,
        )
    }

    companion object {
        fun formatUhrzeit(minuten: Int): String {
            val m = ((minuten % 1440) + 1440) % 1440
            return "%d:%02d".format(m / 60, m % 60)
        }

        /** Formatiert eine Dauer als "h:mm" (ohne Vorzeichen). */
        fun formatDauer(minuten: Int): String {
            val m = kotlin.math.abs(minuten)
            return "%d:%02d".format(m / 60, m % 60)
        }

        /** Formatiert einen Saldo mit Vorzeichen, z. B. "+0:12" oder "−1:05". */
        fun formatSaldo(minuten: Int): String =
            (if (minuten < 0) "−" else "+") + formatDauer(minuten)
    }
}
