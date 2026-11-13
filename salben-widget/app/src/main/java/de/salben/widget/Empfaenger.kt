package de.salben.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import de.salben.widget.widget.SalbenWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Erlaubt einem BroadcastReceiver, kurz im Hintergrund zu arbeiten (Speichern, Widget aktualisieren). */
fun BroadcastReceiver.imHintergrund(arbeit: suspend () -> Unit) {
    val ergebnis = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            arbeit()
        } catch (e: Exception) {
            Log.e("SalbenWidget", "Fehler im Hintergrund", e)
        } finally {
            ergebnis.finish()
        }
    }
}

/** Wird vom AlarmManager und vom "Erledigt"-Knopf der Benachrichtigung aufgerufen. */
class AlarmEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        imHintergrund {
            when (intent.action) {
                Alarme.AKTION_TERMIN -> {
                    val schluessel = intent.getStringExtra(Alarme.EXTRA_SCHLUESSEL)
                    val daten = SalbenSpeicher.laden(app)
                    val termin = daten.termine.find { it.schluessel == schluessel }
                    // Nur erinnern, wenn der Termin noch nicht abgehakt ist
                    if (termin != null && termin.schluessel !in daten.erledigtAm(LocalDate.now())) {
                        Benachrichtigungen.zeigen(app, termin)
                    }
                    Alarme.alleNeuPlanen(app) // Alarm für morgen stellen
                }

                Alarme.AKTION_MITTERNACHT -> {
                    SalbenSpeicher.tagZuruecksetzen(app)
                    Benachrichtigungen.alleEntfernen(app)
                    Alarme.alleNeuPlanen(app)
                }

                Benachrichtigungen.AKTION_ERLEDIGT -> {
                    val schluessel = intent.getStringExtra(Alarme.EXTRA_SCHLUESSEL) ?: return@imHintergrund
                    Benachrichtigungen.entfernen(app, schluessel)
                    val vonHeute = intent.getStringExtra(Benachrichtigungen.EXTRA_DATUM) == LocalDate.now().toString()
                    val gibtEsNoch = SalbenSpeicher.laden(app).termine.any { it.schluessel == schluessel }
                    if (vonHeute && gibtEsNoch) SalbenSpeicher.setzen(app, schluessel, true)
                }
            }
            SalbenWidget().updateAll(app)
        }
    }
}

/** Nach Neustart, App-Update, Zeitumstellung oder neuer Alarm-Erlaubnis: alles neu planen. */
class NeustartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        imHintergrund {
            Alarme.alleNeuPlanen(app)
            SalbenWidget().updateAll(app)
        }
    }
}
