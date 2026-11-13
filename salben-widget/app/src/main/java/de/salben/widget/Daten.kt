package de.salben.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

// =====================================================================
//  HIER kannst du die STANDARDWERTE ändern.
//  Sie gelten, solange du in der App noch nichts gespeichert hast.
//  Bequemer geht es über den Einstellungsbildschirm in der App.
// =====================================================================
object Standardwerte {
    const val SALBE_A_NAME = "Salbe A"
    const val SALBE_A_ZEITEN = "07:00, 14:30, 22:30"

    const val SALBE_B_NAME = "Salbe B"
    const val SALBE_B_ZEITEN = "09:00, 21:00"
}
// =====================================================================

/** Alles, was die App gespeichert hat. */
data class Daten(
    val salbeAName: String,
    val salbeAZeiten: List<LocalTime>,
    val salbeBName: String,
    val salbeBZeiten: List<LocalTime>,
    private val erledigtDatum: String?,
    private val erledigtRoh: Set<String>,
) {
    val termine: List<Termin> = Zeitplan.termine(salbeAName, salbeAZeiten, salbeBName, salbeBZeiten)

    /** Häkchen gelten nur für den Tag, an dem sie gesetzt wurden. Am nächsten Tag ist die Liste leer. */
    fun erledigtAm(tag: LocalDate): Set<String> =
        if (erledigtDatum == tag.toString()) erledigtRoh else emptySet()
}

private val Context.dataStore by preferencesDataStore(name = "salben")

/** Liest und schreibt die gespeicherten Daten (Jetpack DataStore). */
object SalbenSpeicher {
    private val NAME_A = stringPreferencesKey("salbe_a_name")
    private val ZEITEN_A = stringPreferencesKey("salbe_a_zeiten")
    private val NAME_B = stringPreferencesKey("salbe_b_name")
    private val ZEITEN_B = stringPreferencesKey("salbe_b_zeiten")
    private val ERLEDIGT = stringSetPreferencesKey("erledigt")
    private val ERLEDIGT_DATUM = stringPreferencesKey("erledigt_datum")

    fun datenFlow(context: Context): Flow<Daten> =
        context.applicationContext.dataStore.data.map { p -> umwandeln(p) }

    suspend fun laden(context: Context): Daten = datenFlow(context).first()

    private fun umwandeln(p: Preferences) = Daten(
        salbeAName = p[NAME_A] ?: Standardwerte.SALBE_A_NAME,
        salbeAZeiten = Zeitplan.zeitenLesen(p[ZEITEN_A] ?: Standardwerte.SALBE_A_ZEITEN) ?: emptyList(),
        salbeBName = p[NAME_B] ?: Standardwerte.SALBE_B_NAME,
        salbeBZeiten = Zeitplan.zeitenLesen(p[ZEITEN_B] ?: Standardwerte.SALBE_B_ZEITEN) ?: emptyList(),
        erledigtDatum = p[ERLEDIGT_DATUM],
        erledigtRoh = p[ERLEDIGT] ?: emptySet(),
    )

    /** Hakt einen Termin ab (erledigt = true) oder nimmt das Häkchen zurück (false). */
    suspend fun setzen(context: Context, schluessel: String, erledigt: Boolean) {
        context.applicationContext.dataStore.edit { p ->
            val heute = LocalDate.now().toString()
            val bisher = if (p[ERLEDIGT_DATUM] == heute) p[ERLEDIGT] ?: emptySet() else emptySet()
            p[ERLEDIGT_DATUM] = heute
            p[ERLEDIGT] = if (erledigt) bisher + schluessel else bisher - schluessel
        }
    }

    /** Wechselt das Häkchen. Gibt zurück, ob der Termin danach abgehakt ist. */
    suspend fun umschalten(context: Context, schluessel: String): Boolean {
        var jetztErledigt = false
        context.applicationContext.dataStore.edit { p ->
            val heute = LocalDate.now().toString()
            val bisher = if (p[ERLEDIGT_DATUM] == heute) p[ERLEDIGT] ?: emptySet() else emptySet()
            jetztErledigt = schluessel !in bisher
            p[ERLEDIGT_DATUM] = heute
            p[ERLEDIGT] = if (jetztErledigt) bisher + schluessel else bisher - schluessel
        }
        return jetztErledigt
    }

    /** Um Mitternacht: alle Häkchen weg. */
    suspend fun tagZuruecksetzen(context: Context) {
        context.applicationContext.dataStore.edit { p ->
            p[ERLEDIGT_DATUM] = LocalDate.now().toString()
            p[ERLEDIGT] = emptySet()
        }
    }

    suspend fun einstellungenSpeichern(
        context: Context,
        nameA: String, zeitenA: List<LocalTime>,
        nameB: String, zeitenB: List<LocalTime>,
    ) {
        context.applicationContext.dataStore.edit { p ->
            p[NAME_A] = nameA.trim()
            p[ZEITEN_A] = Zeitplan.zeitenText(zeitenA)
            p[NAME_B] = nameB.trim()
            p[ZEITEN_B] = Zeitplan.zeitenText(zeitenB)
        }
    }
}
