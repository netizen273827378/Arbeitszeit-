package de.salben.widget

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** Ein einzelner Termin, z. B. "Salbe A um 7:00". */
data class Termin(
    /** Eindeutiger Name des Termins, z. B. "A-07:00". Darunter wird das Häkchen gespeichert. */
    val schluessel: String,
    val salbe: String,
    val zeit: LocalTime,
)

enum class TerminStatus { ERLEDIGT, UEBERFAELLIG, OFFEN }

/**
 * Reine Zeit-Logik ohne Android-Abhängigkeiten.
 * Dadurch lässt sie sich mit normalen Unit-Tests prüfen (siehe ZeitplanTest.kt).
 */
object Zeitplan {

    /** Liest "07:00, 14:30, 22:30" ein. Gibt null zurück, wenn etwas nicht stimmt. */
    fun zeitenLesen(text: String): List<LocalTime>? {
        val teile = text.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (teile.isEmpty()) return null
        val zeiten = teile.map { zeitLesen(it) ?: return null }
        return zeiten.distinct().sorted()
    }

    /** Liest eine Uhrzeit wie "7:00", "07:00" oder "14.30". */
    fun zeitLesen(text: String): LocalTime? {
        val teile = text.trim().split(':', '.')
        if (teile.size != 2 || teile[1].length != 2) return null
        val stunde = teile[0].toIntOrNull() ?: return null
        val minute = teile[1].toIntOrNull() ?: return null
        if (stunde !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(stunde, minute)
    }

    /** "07:00" – so wird die Zeit gespeichert. */
    fun speicherText(zeit: LocalTime): String =
        String.format(Locale.ROOT, "%02d:%02d", zeit.hour, zeit.minute)

    /** "7:00" – so wird die Zeit angezeigt. */
    fun anzeigen(zeit: LocalTime): String =
        String.format(Locale.ROOT, "%d:%02d", zeit.hour, zeit.minute)

    /** "07:00, 14:30, 22:30" – für das Eingabefeld in den Einstellungen. */
    fun zeitenText(zeiten: List<LocalTime>): String = zeiten.joinToString(", ") { speicherText(it) }

    /** Baut aus beiden Salben die sortierte Tagesliste. */
    fun termine(
        salbeA: String, zeitenA: List<LocalTime>,
        salbeB: String, zeitenB: List<LocalTime>,
    ): List<Termin> {
        val a = zeitenA.map { Termin("A-${speicherText(it)}", salbeA, it) }
        val b = zeitenB.map { Termin("B-${speicherText(it)}", salbeB, it) }
        return (a + b).sortedWith(compareBy({ it.zeit }, { it.schluessel }))
    }

    fun status(termin: Termin, erledigt: Set<String>, jetzt: LocalTime): TerminStatus = when {
        termin.schluessel in erledigt -> TerminStatus.ERLEDIGT
        !termin.zeit.isAfter(jetzt) -> TerminStatus.UEBERFAELLIG
        else -> TerminStatus.OFFEN
    }

    /** Der früheste noch nicht abgehakte Termin (kann auch überfällig sein). */
    fun naechsterOffener(termine: List<Termin>, erledigt: Set<String>): Termin? =
        termine.firstOrNull { it.schluessel !in erledigt }

    /** Wann soll der Alarm für diese Uhrzeit das nächste Mal klingeln? Heute, oder morgen, falls schon vorbei. */
    fun naechsterZeitpunkt(zeit: LocalTime, jetzt: LocalDateTime): LocalDateTime {
        val heute = jetzt.toLocalDate().atTime(zeit)
        return if (heute.isAfter(jetzt)) heute else heute.plusDays(1)
    }

    /** Kurz nach Mitternacht: dann werden alle Häkchen zurückgesetzt. */
    fun naechsteMitternacht(jetzt: LocalDateTime): LocalDateTime =
        jetzt.toLocalDate().plusDays(1).atTime(0, 0, 5)
}
