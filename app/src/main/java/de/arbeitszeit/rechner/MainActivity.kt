package de.arbeitszeit.rechner

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import de.arbeitszeit.rechner.ArbeitszeitRechner.Companion.formatDauer
import de.arbeitszeit.rechner.ArbeitszeitRechner.Companion.formatSaldo
import de.arbeitszeit.rechner.ArbeitszeitRechner.Companion.formatUhrzeit
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : Activity() {

    private val rechner = ArbeitszeitRechner()
    private val handler = Handler(Looper.getMainLooper())

    private var einstempeln: Int? = null
    private var ausstempeln: Int? = null

    private lateinit var textEinstempeln: TextView
    private lateinit var karteErgebnis: LinearLayout
    private lateinit var textFruehestens: TextView
    private lateinit var textRestzeit: TextView
    private lateinit var textDetails: TextView
    private lateinit var textHinweise: TextView
    private lateinit var karteSaldo: LinearLayout
    private lateinit var textAusstempeln: TextView
    private lateinit var textSaldo: TextView

    /** Aktualisiert die Restzeit-Anzeige jede halbe Minute. */
    private val ticker = object : Runnable {
        override fun run() {
            aktualisiereRestzeit()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textEinstempeln = findViewById(R.id.textEinstempeln)
        karteErgebnis = findViewById(R.id.karteErgebnis)
        textFruehestens = findViewById(R.id.textFruehestens)
        textRestzeit = findViewById(R.id.textRestzeit)
        textDetails = findViewById(R.id.textDetails)
        textHinweise = findViewById(R.id.textHinweise)
        karteSaldo = findViewById(R.id.karteSaldo)
        textAusstempeln = findViewById(R.id.textAusstempeln)
        textSaldo = findViewById(R.id.textSaldo)

        val einstempelnWaehlen = View.OnClickListener {
            zeigeZeitauswahl(einstempeln ?: jetzt()) { setzeEinstempeln(it) }
        }
        textEinstempeln.setOnClickListener(einstempelnWaehlen)
        findViewById<Button>(R.id.buttonEinstempelnWaehlen).setOnClickListener(einstempelnWaehlen)
        findViewById<Button>(R.id.buttonEinstempelnJetzt).setOnClickListener { setzeEinstempeln(jetzt()) }

        val ausstempelnWaehlen = View.OnClickListener {
            val vorschlag = ausstempeln
                ?: einstempeln?.let { rechner.berechne(it).fruehestesAusstempeln }
                ?: jetzt()
            zeigeZeitauswahl(vorschlag) { setzeAusstempeln(it) }
        }
        textAusstempeln.setOnClickListener(ausstempelnWaehlen)
        findViewById<Button>(R.id.buttonAusstempelnWaehlen).setOnClickListener(ausstempelnWaehlen)

        ladeGespeicherteZeiten()
        aktualisiereAnzeige()
    }

    override fun onResume() {
        super.onResume()
        // Falls die App über Nacht offen war: gespeicherte Zeit von gestern verwerfen.
        ladeGespeicherteZeiten()
        aktualisiereAnzeige()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun jetzt(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    private fun zeigeZeitauswahl(start: Int, ergebnis: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, stunde, minute -> ergebnis(stunde * 60 + minute) },
            start / 60,
            start % 60,
            true,
        ).show()
    }

    private fun setzeEinstempeln(minuten: Int) {
        einstempeln = minuten
        ausstempeln = null
        speichereZeiten()
        aktualisiereAnzeige()
    }

    private fun setzeAusstempeln(minuten: Int) {
        ausstempeln = minuten
        speichereZeiten()
        aktualisiereAnzeige()
    }

    private fun aktualisiereAnzeige() {
        val ein = einstempeln
        if (ein == null) {
            textEinstempeln.text = "--:--"
            karteErgebnis.visibility = View.GONE
            karteSaldo.visibility = View.GONE
            return
        }

        textEinstempeln.text = formatUhrzeit(ein)
        karteErgebnis.visibility = View.VISIBLE
        karteSaldo.visibility = View.VISIBLE

        val e = rechner.berechne(ein)
        textFruehestens.text = formatUhrzeit(e.fruehestesAusstempeln) + " Uhr"

        textDetails.text = buildString {
            append("Gezählt ab: ${formatUhrzeit(e.angerechneterBeginn)} Uhr (inkl. 15 Min. Gutschrift)\n")
            append("Sollzeit: 7:48 Std. + 0:30 Pause\n")
            append("Saldo bei frühestem Ausstempeln: ${formatSaldo(e.saldoBeiFruehestemAusstempeln)} Std.\n")
            append("Spätestens (max. 10 Std. Arbeit): ${formatUhrzeit(e.spaetestesAusstempeln)} Uhr")
        }

        if (e.hinweise.isEmpty()) {
            textHinweise.visibility = View.GONE
        } else {
            textHinweise.visibility = View.VISIBLE
            textHinweise.text = e.hinweise.joinToString("\n\n") { "⚠ $it" }
        }

        val aus = ausstempeln
        if (aus == null) {
            textAusstempeln.text = "--:--"
            textSaldo.text = ""
        } else {
            textAusstempeln.text = formatUhrzeit(aus)
            val gearbeitet = rechner.gearbeiteteMinuten(ein, aus)
            val saldo = rechner.saldo(ein, aus)
            textSaldo.text = "Gearbeitet: ${formatDauer(gearbeitet)} Std.\nSaldo: ${formatSaldo(saldo)} Std."
            textSaldo.setTextColor(getColor(if (saldo < 0) R.color.minus else R.color.plus))
        }

        aktualisiereRestzeit()
    }

    private fun aktualisiereRestzeit() {
        val ein = einstempeln ?: return
        val ziel = rechner.berechne(ein).fruehestesAusstempeln
        val rest = ziel - jetzt()
        textRestzeit.text = if (rest > 0) {
            "Noch ${formatDauer(rest)} Std. bis Feierabend"
        } else {
            "Du kannst ausstempeln 🎉"
        }
    }

    // --- Speichern, damit die Einstempelzeit beim erneuten Öffnen erhalten bleibt (nur für heute) ---

    private fun prefs() = getSharedPreferences("arbeitszeit", Context.MODE_PRIVATE)

    private fun speichereZeiten() {
        prefs().edit()
            .putString(KEY_DATUM, LocalDate.now().toString())
            .putInt(KEY_EIN, einstempeln ?: -1)
            .putInt(KEY_AUS, ausstempeln ?: -1)
            .apply()
    }

    private fun ladeGespeicherteZeiten() {
        val p = prefs()
        if (p.getString(KEY_DATUM, null) == LocalDate.now().toString()) {
            einstempeln = p.getInt(KEY_EIN, -1).takeIf { it >= 0 }
            ausstempeln = p.getInt(KEY_AUS, -1).takeIf { it >= 0 }
        } else {
            einstempeln = null
            ausstempeln = null
        }
    }

    companion object {
        private const val KEY_DATUM = "datum"
        private const val KEY_EIN = "einstempeln"
        private const val KEY_AUS = "ausstempeln"
    }
}
