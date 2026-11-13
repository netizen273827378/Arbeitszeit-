package de.salben.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Stellt für jeden Termin einen Alarm (AlarmManager) und zusätzlich einen Alarm kurz nach Mitternacht.
 * Nach jedem Alarm werden alle Alarme neu berechnet, so klingelt jeder Termin jeden Tag.
 */
object Alarme {
    const val AKTION_TERMIN = "de.salben.widget.TERMIN"
    const val AKTION_MITTERNACHT = "de.salben.widget.MITTERNACHT"
    const val EXTRA_SCHLUESSEL = "schluessel"

    private const val MAX_TERMINE = 50
    private const val CODE_MITTERNACHT = 1000

    /** Darf die App minutengenaue Alarme stellen? (Ab Android 12 muss man das erlauben.) */
    fun exakteAlarmeErlaubt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    suspend fun alleNeuPlanen(context: Context) {
        val app = context.applicationContext
        val daten = SalbenSpeicher.laden(app)
        val alarmManager = app.getSystemService(AlarmManager::class.java)

        // Alte Alarme löschen (z. B. falls du Uhrzeiten entfernt hast)
        for (code in 0 until MAX_TERMINE) {
            pendingIntent(app, AKTION_TERMIN, code, null, PendingIntent.FLAG_NO_CREATE)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        val jetzt = LocalDateTime.now()
        daten.termine.take(MAX_TERMINE).forEachIndexed { index, termin ->
            val pi = pendingIntent(app, AKTION_TERMIN, index, termin.schluessel, PendingIntent.FLAG_UPDATE_CURRENT)
            if (pi != null) stellen(app, alarmManager, Zeitplan.naechsterZeitpunkt(termin.zeit, jetzt), pi)
        }

        val mitternacht = pendingIntent(app, AKTION_MITTERNACHT, CODE_MITTERNACHT, null, PendingIntent.FLAG_UPDATE_CURRENT)
        if (mitternacht != null) stellen(app, alarmManager, Zeitplan.naechsteMitternacht(jetzt), mitternacht)
    }

    private fun pendingIntent(
        context: Context, aktion: String, code: Int, schluessel: String?, flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, AlarmEmpfaenger::class.java).setAction(aktion)
        if (schluessel != null) intent.putExtra(EXTRA_SCHLUESSEL, schluessel)
        return PendingIntent.getBroadcast(context, code, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun stellen(context: Context, alarmManager: AlarmManager, zeitpunkt: LocalDateTime, pi: PendingIntent) {
        val millis = zeitpunkt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (exakteAlarmeErlaubt(context)) {
                // Pünktlich auf die Minute, auch im Energiesparmodus
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                // Ohne Erlaubnis: ungefähr pünktlich (kann einige Minuten später kommen)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }
}
