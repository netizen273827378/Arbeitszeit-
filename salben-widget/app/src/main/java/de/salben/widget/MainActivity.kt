package de.salben.widget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import de.salben.widget.widget.SalbenWidget
import de.salben.widget.widget.SalbenWidgetReceiver
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Der Einstellungsbildschirm der App. */
class MainActivity : ComponentActivity() {

    /** Zählt hoch, wenn du in die App zurückkehrst – dann wird der Status der Berechtigungen neu geprüft. */
    private var rueckkehr by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Benachrichtigungen.kanalAnlegen(this)
        setContent {
            SalbenTheme {
                EinstellungsBildschirm(rueckkehr)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rueckkehr++
        // Bei jedem Öffnen sicherheitshalber alle Alarme neu stellen und das Widget auffrischen
        lifecycleScope.launch {
            Alarme.alleNeuPlanen(applicationContext)
            SalbenWidget().updateAll(applicationContext)
        }
    }
}

@Composable
fun SalbenTheme(inhalt: @Composable () -> Unit) {
    val dunkel = isSystemInDarkTheme()
    val context = LocalContext.current
    val farben = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dunkel) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dunkel -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = farben, content = inhalt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinstellungsBildschirm(rueckkehr: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Eingabefelder
    var nameA by remember { mutableStateOf("") }
    var zeitenA by remember { mutableStateOf("") }
    var nameB by remember { mutableStateOf("") }
    var zeitenB by remember { mutableStateOf("") }
    var geladen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val d = SalbenSpeicher.laden(context)
        nameA = d.salbeAName
        zeitenA = Zeitplan.zeitenText(d.salbeAZeiten)
        nameB = d.salbeBName
        zeitenB = Zeitplan.zeitenText(d.salbeBZeiten)
        geladen = true
    }

    // Status der Berechtigungen
    var neuPruefen by remember { mutableIntStateOf(0) }
    val benachrichtigungenOk = remember(rueckkehr, neuPruefen) { Benachrichtigungen.erlaubt(context) }
    val alarmeOk = remember(rueckkehr, neuPruefen) { Alarme.exakteAlarmeErlaubt(context) }

    var vomKnopf by remember { mutableStateOf(false) }
    val erlaubnisFragen = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erteilt ->
        neuPruefen++
        // Wenn Android nicht mehr fragt (zweimal abgelehnt), die Einstellungen öffnen
        if (!erteilt && vomKnopf) benachrichtigungsEinstellungenOeffnen(context)
        vomKnopf = false
    }

    // Beim ersten Start einmal nach Benachrichtigungen fragen
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Benachrichtigungen.erlaubt(context)) {
            erlaubnisFragen.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val fehlerA = geladen && (Zeitplan.zeitenLesen(zeitenA) == null)
    val fehlerB = geladen && (Zeitplan.zeitenLesen(zeitenB) == null)

    Scaffold(topBar = { TopAppBar(title = { Text("Salben-Erinnerung") }) }) { innen ->
        Column(
            modifier = Modifier
                .padding(innen)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---------- Berechtigungen ----------
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Berechtigungen", style = MaterialTheme.typography.titleMedium)
                    BerechtigungsZeile(
                        text = "Benachrichtigungen",
                        ok = benachrichtigungenOk,
                        onErlauben = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS,
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                vomKnopf = true
                                erlaubnisFragen.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                benachrichtigungsEinstellungenOeffnen(context)
                            }
                        },
                    )
                    BerechtigungsZeile(
                        text = "Pünktliche Alarme",
                        ok = alarmeOk,
                        onErlauben = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                                )
                            }
                        },
                    )
                }
            }

            // ---------- Salben ----------
            SalbenKarte("Salbe A", nameA, { nameA = it }, zeitenA, { zeitenA = it }, fehlerA, "z. B. 07:00, 14:30, 22:30")
            SalbenKarte("Salbe B", nameB, { nameB = it }, zeitenB, { zeitenB = it }, fehlerB, "z. B. 09:00, 21:00")

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = geladen,
                onClick = {
                    val zA = Zeitplan.zeitenLesen(zeitenA)
                    val zB = Zeitplan.zeitenLesen(zeitenB)
                    if (zA == null || zB == null || nameA.isBlank() || nameB.isBlank()) {
                        Toast.makeText(context, "Bitte Namen und Uhrzeiten prüfen (Format 07:00)", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    scope.launch {
                        SalbenSpeicher.einstellungenSpeichern(context, nameA, zA, nameB, zB)
                        Alarme.alleNeuPlanen(context)
                        SalbenWidget().updateAll(context)
                        zeitenA = Zeitplan.zeitenText(zA)
                        zeitenB = Zeitplan.zeitenText(zB)
                        Toast.makeText(context, "Gespeichert", Toast.LENGTH_SHORT).show()
                    }
                },
            ) { Text("Speichern") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Benachrichtigungen.zeigen(context, Termin("TEST", nameA.ifBlank { "Salbe A" }, LocalTime.now().withSecond(0).withNano(0)))
                    if (!Benachrichtigungen.erlaubt(context)) {
                        Toast.makeText(context, "Benachrichtigungen sind noch nicht erlaubt", Toast.LENGTH_LONG).show()
                    }
                },
            ) { Text("Test-Benachrichtigung senden") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { widgetAnheften(context) },
            ) { Text("Widget zum Startbildschirm hinzufügen") }

            Text(
                "Tipp: Im Widget einen Termin antippen, um ihn abzuhaken. Nochmal antippen nimmt das Häkchen zurück. " +
                    "Um Mitternacht werden alle Häkchen automatisch entfernt.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BerechtigungsZeile(text: String, ok: Boolean, onErlauben: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text, fontWeight = FontWeight.Medium)
            Text(
                if (ok) "✓ erlaubt" else "✗ nicht erlaubt",
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!ok) Button(onClick = onErlauben) { Text("Erlauben") }
    }
}

@Composable
private fun SalbenKarte(
    titel: String,
    name: String, nameAendern: (String) -> Unit,
    zeiten: String, zeitenAendern: (String) -> Unit,
    fehler: Boolean, beispiel: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titel, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name, onValueChange = nameAendern,
                label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = zeiten, onValueChange = zeitenAendern,
                label = { Text("Uhrzeiten") }, singleLine = true,
                isError = fehler,
                supportingText = { Text(if (fehler) "Format: 07:00, 14:30 (durch Komma getrennt)" else beispiel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun benachrichtigungsEinstellungenOeffnen(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}

private fun widgetAnheften(context: Context) {
    val manager = context.getSystemService(AppWidgetManager::class.java)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, SalbenWidgetReceiver::class.java), null, null)
    } else {
        Toast.makeText(
            context,
            "Bitte lange auf den Startbildschirm drücken → Widgets → Salben-Erinnerung",
            Toast.LENGTH_LONG,
        ).show()
    }
}
