# Salben-Erinnerung: Anleitung für Einsteigerinnen

Homescreen-Widget für zwei Salben, mit Abhaken, Benachrichtigungen und automatischem Zurücksetzen um Mitternacht.

| Salbe | Uhrzeiten (Standard) |
|---|---|
| Salbe A | 7:00 · 14:30 · 22:30 |
| Salbe B | 9:00 · 21:00 |

Namen und Uhrzeiten änderst du in der App unter **Einstellungen → Speichern**.
Die Standardwerte stehen außerdem gut markiert in `app/src/main/java/de/salben/widget/Daten.kt` (Abschnitt `Standardwerte`).

---

## 1. Android Studio installieren

1. Öffne <https://developer.android.com/studio> und lade Android Studio herunter.
2. Installiere es mit den vorgeschlagenen Einstellungen („Standard“). Beim ersten Start lädt Android Studio das Android-SDK herunter. Das dauert ein paar Minuten.
3. Lizenzfragen mit **Accept** bestätigen und am Ende auf **Finish** klicken.

## 2. Projekt öffnen

### Weg A (empfohlen): fertiges Projekt öffnen
1. Lade das Projekt von GitHub herunter: Repository öffnen → Branch auswählen → grüner Knopf **Code → Download ZIP**.
2. Entpacke die ZIP-Datei, zum Beispiel nach `Dokumente/`.
3. In Android Studio: **File → Open** (oder auf dem Startbildschirm **Open**) → wähle den Ordner **`salben-widget`** (nicht den äußeren Ordner!) → **OK**.
4. Fragt Android Studio „Trust Project?“, antwortest du mit **Trust Project**.
5. Unten rechts läuft jetzt „Gradle Sync“. Warte, bis die Meldung verschwunden ist (beim ersten Mal 2 bis 10 Minuten).

### Weg B: neues Projekt anlegen und Dateien hineinkopieren
1. **File → New → New Project… → Empty Activity** → Next.
2. Name: `SalbenWidget`, Package name: **`de.salben.widget`**, Language: Kotlin, Minimum SDK: **API 26**, Build configuration language: **Kotlin DSL** → Finish.
3. Schalte links oben die Ansicht von „Android“ auf **„Project“** um. Dann siehst du die echten Ordner.
4. Ersetze bzw. erstelle diese Dateien mit dem Inhalt aus diesem Projekt. Neue Ordner legst du per Rechtsklick → New → Directory an, neue Dateien per Rechtsklick → New → File:

```
SalbenWidget/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/de/salben/widget/
        │   │   ├── MainActivity.kt          ← Einstellungsbildschirm
        │   │   ├── Daten.kt                 ← Speicherung + STANDARDWERTE
        │   │   ├── Zeitplan.kt              ← Zeitlogik
        │   │   ├── Alarme.kt                ← AlarmManager
        │   │   ├── Benachrichtigungen.kt    ← "Zeit für Salbe A"
        │   │   ├── Empfaenger.kt            ← Alarm-, Erledigt- und Neustart-Empfänger
        │   │   └── widget/
        │   │       └── SalbenWidget.kt      ← das Widget
        │   └── res/
        │       ├── drawable/  (ic_benachrichtigung, ic_haken, ic_kaestchen_haken,
        │       │               ic_kaestchen_leer, ic_launcher_vordergrund, widget_hintergrund)
        │       ├── mipmap-anydpi-v26/ic_launcher.xml
        │       ├── values/        (colors.xml, strings.xml, themes.xml)
        │       ├── values-night/  (colors.xml, themes.xml)   ← dunkler Modus
        │       └── xml/salben_widget_info.xml
        └── test/java/de/salben/widget/ZeitplanTest.kt
```

5. **Lösche** alles, was Android Studio zusätzlich angelegt hat und nicht in der Liste steht. Sonst gibt es Konflikte. Betroffen sind vor allem der Ordner `ui/theme/`, die Dateien in `mipmap-*dpi`, `ic_launcher_background.xml`/`ic_launcher_foreground.xml`, `values/themes.xml` (durch unsere ersetzen) und `gradle/libs.versions.toml`.
6. Oben erscheint ein Balken „Gradle files have changed“ → **Sync Now**.

Weg A ist deutlich einfacher und weniger fehleranfällig.

## 3. Handy vorbereiten: Entwickleroptionen und USB-Debugging

1. Handy: **Einstellungen → Über das Telefon** (bei Samsung: *Telefoninfo → Softwareinformationen*).
2. Tippe **7-mal** auf **Build-Nummer**. Es erscheint „Sie sind jetzt Entwickler“. Eventuell musst du deine PIN eingeben.
3. Zurück zu **Einstellungen → System → Entwickleroptionen** (bei Samsung direkt unten in den Einstellungen).
4. Schalte **USB-Debugging** ein.
5. Verbinde das Handy per USB-Kabel mit dem Computer. Nimm ein Datenkabel, denn manche Kabel können nur laden.
6. Auf dem Handy erscheint „USB-Debugging zulassen?“ → Haken bei **„Von diesem Computer immer zulassen“** → **Zulassen**.
7. Falls das Handy nach dem USB-Modus fragt: **Dateiübertragung** wählen.

## 4. App bauen und installieren

1. In Android Studio oben in der Werkzeugleiste: Im Geräte-Auswahlfeld sollte jetzt dein Handy stehen (z. B. „Pixel 8“).
2. Links daneben muss **app** ausgewählt sein.
3. Klicke auf den grünen **▶ Run**-Knopf (oder drücke Umschalt+F10).
4. Android Studio baut die App (beim ersten Mal 1 bis 3 Minuten) und installiert sie. Die App öffnet sich automatisch auf dem Handy.

## 5. Berechtigungen erteilen

Beim ersten Start fragt die App: **„Salben-Erinnerung Benachrichtigungen senden lassen?“** → **Zulassen**.

In der App siehst du oben die Karte **Berechtigungen**:

- **Benachrichtigungen**: Steht dort „✗ nicht erlaubt“, tippe auf **Erlauben**.
- **Pünktliche Alarme**: Tippe auf **Erlauben**. Es öffnet sich „Wecker und Erinnerungen“ → Schalter für *Salben-Erinnerung* **einschalten** → zurück zur App. Danach steht dort „✓ erlaubt“.

Tippe zum Testen auf **Test-Benachrichtigung senden**. Eine Benachrichtigung mit dem Knopf „Erledigt“ sollte erscheinen.

**Wichtig bei Samsung, Xiaomi, Huawei, OnePlus und ähnlichen Geräten:** Diese Hersteller beenden Apps im Hintergrund gern. Damit die Erinnerungen zuverlässig kommen:
Einstellungen → Apps → Salben-Erinnerung → **Akku** → **„Nicht eingeschränkt“** (bzw. „Keine Einschränkungen“).
Mehr dazu gerätespezifisch: <https://dontkillmyapp.com>

## 6. Widget auf den Startbildschirm legen

- **Einfach:** In der App auf **„Widget zum Startbildschirm hinzufügen“** tippen → **Hinzufügen**.
- **Oder von Hand:** Lange auf eine freie Stelle des Startbildschirms drücken → **Widgets** → nach „Salben-Erinnerung“ suchen → **Salben-Plan** auf den Startbildschirm ziehen.
- Die Größe änderst du, indem du lange auf das Widget drückst und an den Rändern ziehst. Ideal sind etwa 4 × 3 Felder.

**Bedienung:**
- Tipp auf einen Termin → abgehakt (grau und durchgestrichen). Erneuter Tipp → Häkchen weg.
- Rot = Uhrzeit vorbei, aber noch nicht abgehakt.
- Tipp auf die große Überschrift → App öffnet sich.
- „Erledigt“ in der Benachrichtigung hakt den Termin auch im Widget ab.
- Um Mitternacht werden alle Häkchen entfernt.

---

## 7. Typische Fehler und Lösungen

| Problem | Lösung |
|---|---|
| **Handy erscheint nicht in der Geräteliste** | Anderes USB-Kabel oder anderen USB-Anschluss probieren. USB-Debugging aus- und wieder einschalten. Am Handy die Frage „USB-Debugging zulassen?“ bestätigen (Handy entsperren!). Unter Windows ggf. den USB-Treiber des Herstellers installieren (z. B. *Samsung USB Driver*). |
| **„Gradle sync failed“ / „Could not resolve …“** | Internetverbindung prüfen, dann **File → Sync Project with Gradle Files**. Hilft das nicht: **File → Invalidate Caches… → Invalidate and Restart**. |
| **„Unsupported Java“ / „JDK … is not compatible“** | **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** auf **„jbr-21“** bzw. „Embedded JDK“ stellen. |
| **Android Studio schlägt „Upgrade Android Gradle Plugin“ vor** | Kannst du ignorieren („Remind me later“ / „Don't ask for this project“). Das Projekt funktioniert mit den eingetragenen Versionen. |
| **„SDK location not found“** | Projekt einmal schließen und über **File → Open** neu öffnen. Android Studio legt dann `local.properties` an. Notfalls unter **Tools → SDK Manager** prüfen, dass „Android 15 (API 35)“ installiert ist. |
| **„Unresolved reference …“ (Weg B)** | Steht in jeder Kotlin-Datei ganz oben `package de.salben.widget` (bzw. `de.salben.widget.widget` in `SalbenWidget.kt`)? Liegt jede Datei im richtigen Ordner? Hast du alte Dateien wie `ui/theme/` gelöscht? |
| **„Duplicate resources“ / „Duplicate class“ (Weg B)** | Eine Datei existiert doppelt, z. B. `ic_launcher.webp` neben `ic_launcher.xml`. Die automatisch erzeugten Dateien löschen. |
| **„INSTALL_FAILED_UPDATE_INCOMPATIBLE“** | Eine ältere App mit gleichem Namen ist installiert. Sie am Handy deinstallieren und erneut ▶ Run drücken. |
| **Widget taucht nicht in der Widget-Liste auf** | App einmal öffnen, Handy ggf. neu starten. Manche Launcher brauchen nach der ersten Installation etwas. |
| **Keine Benachrichtigungen** | In der App müssen beide Berechtigungen „✓ erlaubt“ zeigen. Akku-Optimierung für die App ausschalten (siehe Abschnitt 5). „Bitte nicht stören“ prüfen. Mit „Test-Benachrichtigung senden“ testen. |
| **Benachrichtigungen kommen einige Minuten zu spät** | „Pünktliche Alarme“ ist nicht erlaubt (siehe Abschnitt 5) oder die Akku-Optimierung ist aktiv. |
| **Widget zeigt nach dem Abhaken noch den alten Stand** | Ein bis zwei Sekunden warten. Manche Launcher aktualisieren verzögert. Notfalls die App kurz öffnen, dann wird das Widget neu gezeichnet. |
| **Nach einem Handy-Neustart keine Erinnerungen** | Bei manchen Herstellern muss „Autostart“ für die App erlaubt werden (Einstellungen → Apps → Salben-Erinnerung → Autostart). Einmal die App öffnen setzt alle Alarme ebenfalls neu. |

## Ohne Android Studio (alternativ)

Bei jedem Push baut GitHub die App automatisch: Repository auf GitHub → Reiter **Actions** → letzter Lauf **„Salben-Widget bauen“** → unten unter *Artifacts* **Salben-Widget-APK** herunterladen. ZIP entpacken, `app-debug.apk` aufs Handy kopieren, antippen und „Installation aus unbekannten Quellen“ erlauben.

## Warum Jetpack Glance?

Glance ist Googles aktuelle Bibliothek für Widgets. Man schreibt das Aussehen in Kotlin statt in XML-Layouts plus `RemoteViews`. Klicks auf einzelne Listeneinträge, der dunkle Modus und das automatische Neuzeichnen nach dem Abhaken sind damit deutlich einfacher als mit dem alten `AppWidgetProvider`. Glance setzt intern selbst auf `AppWidgetProvider` auf und läuft ab Android 8 (API 26).
