# Arbeitszeit-Rechner (Android)

Kleine Android-App, die aus der **Einstempelzeit am Morgen** berechnet, wann man **frühestens ausstempeln** kann, ohne Minusstunden zu machen.

## Regeln

| Regel | Wert |
|---|---|
| Sollarbeitszeit | 7,8 Std. = **7:48** pro Tag (5 × 7,8 = 39 Std./Woche) |
| Kernarbeitszeit | 9:00 – 15:00 |
| Gleitzeit | 6:00 – 20:00 |
| Gutschrift fürs Einstempeln | **+15 Min.** (Weg zum Platz, PC hochfahren, anmelden) |
| Mittagspause | pauschal **−30 Min.**, egal wie lange |

## Rechnung

```
gezählt ab           = Einstempelzeit − 15 Min.   (frühestens 6:00)
frühestens Ausstempeln = gezählt ab + 7:48 + 0:30
                     = Einstempelzeit + 8:03
```

zusätzlich:

* nicht vor **15:00** (Ende Kernzeit) – dann entstehen Plusstunden,
* nicht nach **20:00** (Ende Gleitzeit) – dann entstehen Minusstunden.

Beispiele:

| Eingestempelt | Gezählt ab | Frühestens raus | Saldo |
|---|---|---|---|
| 6:15 | 6:00 | 15:00 (Kernzeit) | +0:42 |
| 7:30 | 7:15 | 15:33 | ±0:00 |
| 8:00 | 7:45 | 16:03 | ±0:00 |
| 12:00 | 11:45 | 20:00 (Gleitzeitende) | −0:03 |

## Funktionen

* Einstempelzeit per Uhrzeitauswahl oder Knopf „Jetzt“ eingeben
* Anzeige der frühesten Ausstempelzeit und Restzeit bis Feierabend (aktualisiert sich live)
* Hinweise bei Kernzeit-/Gleitzeit-Grenzen
* Saldo-Rechner: „Wenn ich um … ausstempele“ → Plus-/Minusstunden des Tages
* Spätestes Ausstempeln bei max. 10 Std. Arbeitszeit
* Die Einstempelzeit wird für den aktuellen Tag gespeichert

Die Regeln stehen zentral in `ArbeitszeitRegeln` (`app/src/main/java/de/arbeitszeit/rechner/ArbeitszeitRechner.kt`) und lassen sich dort anpassen.

## APK bekommen

**Ohne Android Studio:** Bei jedem Push baut GitHub Actions automatisch die App.
Auf GitHub → Reiter **Actions** → letzter Lauf „Android-App bauen“ → unten unter *Artifacts* **Arbeitszeit-APK** herunterladen, ZIP entpacken, `app-debug.apk` aufs Handy kopieren und installieren (Installation aus unbekannten Quellen erlauben).

**Mit Android Studio:** Projekt öffnen und auf ▶ klicken, oder:

```
./gradlew assembleDebug      # APK unter app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # Tests der Rechenlogik
```
