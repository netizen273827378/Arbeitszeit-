package de.salben.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.salben.widget.Benachrichtigungen
import de.salben.widget.Daten
import de.salben.widget.MainActivity
import de.salben.widget.R
import de.salben.widget.SalbenSpeicher
import de.salben.widget.Termin
import de.salben.widget.TerminStatus
import de.salben.widget.Zeitplan
import java.time.LocalDateTime

/** Das Widget selbst. Es wird mit Jetpack Glance (Compose-ähnlich) gebaut. */
class SalbenWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val start = SalbenSpeicher.laden(context)
        provideContent {
            // Wenn sich gespeicherte Daten ändern (Häkchen!), zeichnet sich das Widget automatisch neu.
            val fluss = remember { SalbenSpeicher.datenFlow(context) }
            val daten by fluss.collectAsState(initial = start)
            WidgetInhalt(daten, LocalDateTime.now())
        }
    }
}

/** Verbindet das Widget mit Android (steht im AndroidManifest.xml). */
class SalbenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SalbenWidget()
}

/** Wird ausgeführt, wenn du im Widget auf einen Termin tippst. */
class AbhakenAktion : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val schluessel = parameters[SCHLUESSEL] ?: return
        val jetztErledigt = SalbenSpeicher.umschalten(context, schluessel)
        if (jetztErledigt) Benachrichtigungen.entfernen(context, schluessel)
        SalbenWidget().updateAll(context)
    }

    companion object {
        val SCHLUESSEL = ActionParameters.Key<String>("termin")
    }
}

// ---------------------------------------------------------------------
//  Aussehen des Widgets
// ---------------------------------------------------------------------

private fun farbe(id: Int) = ColorProvider(id)

@Composable
private fun WidgetInhalt(daten: Daten, jetzt: LocalDateTime) {
    val erledigt = daten.erledigtAm(jetzt.toLocalDate())
    val termine = daten.termine

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_hintergrund))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Kopfzeile(termine, erledigt, jetzt)
        Spacer(GlanceModifier.height(8.dp))

        if (termine.isEmpty()) {
            Text(
                "Keine Termine eingestellt. Tippe hier, um die App zu öffnen.",
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(color = farbe(R.color.widget_text_gedimmt), fontSize = 14.sp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(termine, itemId = { it.schluessel.hashCode().toLong() }) { termin ->
                    TerminZeile(termin, Zeitplan.status(termin, erledigt, jetzt.toLocalTime()))
                }
            }
        }
    }
}

@Composable
private fun Kopfzeile(termine: List<Termin>, erledigt: Set<String>, jetzt: LocalDateTime) {
    val naechster = Zeitplan.naechsterOffener(termine, erledigt)
    val anzahlErledigt = termine.count { it.schluessel in erledigt }

    val (titel, titelFarbe) = when {
        termine.isEmpty() -> "Salben-Plan" to R.color.widget_text
        naechster == null -> "Für heute alles erledigt ✓" to R.color.widget_akzent
        !naechster.zeit.isAfter(jetzt.toLocalTime()) ->
            "Überfällig: ${naechster.salbe} um ${Zeitplan.anzeigen(naechster.zeit)}" to R.color.widget_rot
        else -> "Nächste: ${naechster.salbe} um ${Zeitplan.anzeigen(naechster.zeit)}" to R.color.widget_text
    }
    val untertitel = when {
        termine.isEmpty() -> ""
        naechster == null -> "Morgen zuerst: ${termine.first().salbe} um ${Zeitplan.anzeigen(termine.first().zeit)}"
        else -> "$anzahlErledigt von ${termine.size} erledigt"
    }

    Column(modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())) {
        Text(
            titel,
            style = TextStyle(color = farbe(titelFarbe), fontSize = 20.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
        )
        if (untertitel.isNotEmpty()) {
            Text(
                untertitel,
                style = TextStyle(color = farbe(R.color.widget_text_gedimmt), fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun TerminZeile(termin: Termin, status: TerminStatus) {
    val farbId = when (status) {
        TerminStatus.ERLEDIGT -> R.color.widget_text_erledigt
        TerminStatus.UEBERFAELLIG -> R.color.widget_rot
        TerminStatus.OFFEN -> R.color.widget_text
    }
    val istErledigt = status == TerminStatus.ERLEDIGT
    val durchgestrichen = if (istErledigt) TextDecoration.LineThrough else TextDecoration.None
    val stil = TextStyle(
        color = farbe(farbId),
        fontSize = 16.sp,
        fontWeight = if (status == TerminStatus.UEBERFAELLIG) FontWeight.Bold else FontWeight.Normal,
        textDecoration = durchgestrichen,
    )
    val zeitStil = TextStyle(
        color = farbe(farbId),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textDecoration = durchgestrichen,
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clickable(actionRunCallback<AbhakenAktion>(actionParametersOf(AbhakenAktion.SCHLUESSEL to termin.schluessel))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(if (istErledigt) R.drawable.ic_kaestchen_haken else R.drawable.ic_kaestchen_leer),
            contentDescription = if (istErledigt) "erledigt" else "offen",
            modifier = GlanceModifier.size(24.dp),
            colorFilter = ColorFilter.tint(farbe(if (istErledigt) R.color.widget_akzent else farbId)),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(Zeitplan.anzeigen(termin.zeit), style = zeitStil, modifier = GlanceModifier.width(54.dp))
        Text(termin.salbe, style = stil, maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (status == TerminStatus.UEBERFAELLIG) {
            Text("überfällig", style = TextStyle(color = farbe(R.color.widget_rot), fontSize = 12.sp))
        }
    }
}
