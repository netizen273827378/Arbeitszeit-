package de.arbeitszeit.rechner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArbeitszeitRechnerTest {

    private val rechner = ArbeitszeitRechner()
    private fun t(h: Int, m: Int) = h * 60 + m

    @Test
    fun normalerTag_7Uhr30() {
        // 7:30 einstempeln -> Zählung ab 7:15, + 7:48 Arbeit + 0:30 Pause = 15:33
        val e = rechner.berechne(t(7, 30))
        assertEquals(t(7, 15), e.angerechneterBeginn)
        assertEquals(t(15, 33), e.fruehestesAusstempeln)
        assertEquals(0, e.saldoBeiFruehestemAusstempeln)
        assertTrue(e.hinweise.isEmpty())
    }

    @Test
    fun einstempeln8Uhr() {
        val e = rechner.berechne(t(8, 0))
        assertEquals(t(16, 3), e.fruehestesAusstempeln)
        assertEquals(0, e.saldoBeiFruehestemAusstempeln)
    }

    @Test
    fun fruehesEinstempeln_KernzeitBegrenzt() {
        // 6:15 einstempeln -> Zählung ab 6:00 -> Soll um 14:18, Kernzeit bis 15:00
        val e = rechner.berechne(t(6, 15))
        assertEquals(t(6, 0), e.angerechneterBeginn)
        assertEquals(t(14, 18), e.sollErreichtUm)
        assertEquals(t(15, 0), e.fruehestesAusstempeln)
        assertEquals(42, e.saldoBeiFruehestemAusstempeln)
    }

    @Test
    fun gutschriftNichtVorGleitzeitbeginn() {
        // 6:05 einstempeln -> nur bis 6:00 zurück angerechnet
        assertEquals(t(6, 0), rechner.angerechneterBeginn(t(6, 5)))
        assertEquals(t(6, 0), rechner.angerechneterBeginn(t(5, 40)))
        assertEquals(t(6, 5), rechner.angerechneterBeginn(t(6, 20)))
    }

    @Test
    fun spaetesEinstempeln_Gleitzeitende() {
        // 12:00 einstempeln -> Soll erst 20:03 -> Gleitzeitende 20:00, 3 Min. minus
        val e = rechner.berechne(t(12, 0))
        assertEquals(t(20, 0), e.fruehestesAusstempeln)
        assertEquals(-3, e.saldoBeiFruehestemAusstempeln)
        assertTrue(e.hinweise.any { it.contains("Minusstunden") })
        assertTrue(e.hinweise.any { it.contains("Kernarbeitszeit") })
    }

    @Test
    fun saldoBerechnung() {
        assertEquals(0, rechner.saldo(t(7, 30), t(15, 33)))
        assertEquals(27, rechner.saldo(t(7, 30), t(16, 0)))
        assertEquals(-33, rechner.saldo(t(7, 30), t(15, 0)))
    }

    @Test
    fun spaetestesAusstempeln10Stunden() {
        // 7:30 -> ab 7:15 + 10:00 + 0:30 = 17:45
        assertEquals(t(17, 45), rechner.berechne(t(7, 30)).spaetestesAusstempeln)
    }

    @Test
    fun formatierung() {
        assertEquals("7:05", ArbeitszeitRechner.formatUhrzeit(t(7, 5)))
        assertEquals("+0:42", ArbeitszeitRechner.formatSaldo(42))
        assertEquals("−1:05", ArbeitszeitRechner.formatSaldo(-65))
    }
}
