package de.salben.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ZeitplanTest {

    private val termine = Zeitplan.termine(
        "Salbe A", listOf(LocalTime.of(7, 0), LocalTime.of(14, 30), LocalTime.of(22, 30)),
        "Salbe B", listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
    )

    @Test
    fun zeitenWerdenGelesenUndSortiert() {
        assertEquals(
            listOf(LocalTime.of(7, 0), LocalTime.of(14, 30), LocalTime.of(22, 30)),
            Zeitplan.zeitenLesen("22:30, 7:00,14.30"),
        )
    }

    @Test
    fun falscheZeitenWerdenAbgelehnt() {
        assertNull(Zeitplan.zeitenLesen("25:00"))
        assertNull(Zeitplan.zeitenLesen("7:5"))
        assertNull(Zeitplan.zeitenLesen("abc"))
        assertNull(Zeitplan.zeitenLesen(""))
    }

    @Test
    fun termineSindNachUhrzeitSortiert() {
        assertEquals(
            listOf("A-07:00", "B-09:00", "A-14:30", "B-21:00", "A-22:30"),
            termine.map { it.schluessel },
        )
    }

    @Test
    fun statusErledigtUeberfaelligOffen() {
        val erledigt = setOf("A-07:00")
        val jetzt = LocalTime.of(10, 0)
        assertEquals(TerminStatus.ERLEDIGT, Zeitplan.status(termine[0], erledigt, jetzt))
        assertEquals(TerminStatus.UEBERFAELLIG, Zeitplan.status(termine[1], erledigt, jetzt))
        assertEquals(TerminStatus.OFFEN, Zeitplan.status(termine[2], erledigt, jetzt))
    }

    @Test
    fun naechsterOffenerTermin() {
        assertEquals("B-09:00", Zeitplan.naechsterOffener(termine, setOf("A-07:00"))?.schluessel)
        assertNull(Zeitplan.naechsterOffener(termine, termine.map { it.schluessel }.toSet()))
    }

    @Test
    fun alarmHeuteOderMorgen() {
        val jetzt = LocalDateTime.of(2026, 3, 10, 10, 0)
        assertEquals(LocalDateTime.of(2026, 3, 10, 14, 30), Zeitplan.naechsterZeitpunkt(LocalTime.of(14, 30), jetzt))
        assertEquals(LocalDateTime.of(2026, 3, 11, 7, 0), Zeitplan.naechsterZeitpunkt(LocalTime.of(7, 0), jetzt))
        // Genau zur Alarmzeit: nächster Alarm ist morgen
        assertEquals(LocalDateTime.of(2026, 3, 11, 10, 0), Zeitplan.naechsterZeitpunkt(LocalTime.of(10, 0), jetzt))
    }

    @Test
    fun mitternacht() {
        assertEquals(
            LocalDateTime.of(2026, 3, 11, 0, 0, 5),
            Zeitplan.naechsteMitternacht(LocalDateTime.of(2026, 3, 10, 23, 59)),
        )
    }
}
