package de.salben.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

/** Zeigt "Zeit für Salbe A" mit dem Knopf "Erledigt". */
object Benachrichtigungen {
    private const val KANAL = "salben_erinnerung"
    const val AKTION_ERLEDIGT = "de.salben.widget.ERLEDIGT"
    const val EXTRA_DATUM = "datum"

    fun kanalAnlegen(context: Context) {
        val kanal = NotificationChannel(KANAL, "Salben-Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Erinnert dich, wenn eine Salbe fällig ist"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }

    /** Darf die App Benachrichtigungen zeigen? */
    fun erlaubt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun id(schluessel: String): Int = schluessel.hashCode()

    fun zeigen(context: Context, termin: Termin) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        kanalAnlegen(context)

        // Tipp auf die Benachrichtigung: App öffnen
        val oeffnen = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        // Knopf "Erledigt": Termin abhaken (das Datum verhindert, dass eine alte Benachrichtigung von gestern den heutigen Termin abhakt)
        val erledigtIntent = Intent(context, AlarmEmpfaenger::class.java)
            .setAction(AKTION_ERLEDIGT)
            .putExtra(Alarme.EXTRA_SCHLUESSEL, termin.schluessel)
            .putExtra(EXTRA_DATUM, LocalDate.now().toString())
        val erledigt = PendingIntent.getBroadcast(
            context, id(termin.schluessel), erledigtIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val benachrichtigung = NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_benachrichtigung)
            .setContentTitle("Zeit für ${termin.salbe}")
            .setContentText("Geplant um ${Zeitplan.anzeigen(termin.zeit)} Uhr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(oeffnen)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_haken, "Erledigt", erledigt)
            .build()

        NotificationManagerCompat.from(context).notify(id(termin.schluessel), benachrichtigung)
    }

    fun entfernen(context: Context, schluessel: String) {
        NotificationManagerCompat.from(context).cancel(id(schluessel))
    }

    fun alleEntfernen(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
