# Meine Termine

Eine eigenständige Android-App für Arzttermine – mit Kategorien, dem nächsten
Termin ganz oben und Erinnerungen, die auch wirklich ankommen.

## Warum nicht einfach der Kalender?

Normale Kalender-Apps planen ihre Benachrichtigungen so, dass Android sie im
Doze-Modus verschieben oder ganz schlucken darf. Diese App nutzt stattdessen
`AlarmManager.setAlarmClock()`. Solche Alarme sind von Doze und Akku-Optimierung
ausgenommen und feuern auf die Minute – genau das, was bei einem Arzttermin
zählt. Nebenwirkung: Android zeigt oben in der Statusleiste ein kleines
Wecker-Symbol, solange eine Erinnerung geplant ist.

Zusätzlich werden alle Alarme nach einem Neustart, einem App-Update und bei
Zeitumstellung automatisch neu gesetzt. Genau daran scheitern viele Apps.

## Funktionen

**Übersicht**

- Der nächste Termin steht groß oben, mit Countdown ("noch 2 Tage 3 Std")
- Alles Weitere in Zeitgruppen: Heute, Morgen, Diese Woche, Nächste Woche, Monate
- Datumsplakette links an jeder Zeile – die Liste liest sich im Vorbeigehen
- Filter nach Kategorie, gewesene Termine einklappbar
- Glockensymbol zeigt, wo eine Erinnerung hängt

**Eintragen**

- Abschnitte statt endloses Formular: Worum geht es, Wann, Erinnerung, Noch etwas
- Kategorie als Farbchip antippen statt aus einer Liste suchen
- Schnellauswahl fürs Datum: Morgen, Übermorgen, In 1 Woche, In 4 Wochen
- Beliebig viele Erinnerungen pro Termin
  - Vorgaben: pünktlich, 15 / 30 / 45 Minuten, 1 / 2 / 3 Stunden, 1 / 2 Tage, 1 Woche
  - frei eingebbare Vorlaufzeit in Minuten, Stunden oder Tagen

**Einstellungen**

- Erinnerungs-Prüfung: zeigt grün oder rot, was Android gerade erlaubt
  (Benachrichtigungen, exakte Alarme, Akku-Optimierung) – jeweils mit einem
  Knopf, der direkt in die richtige Systemeinstellung springt
- Kategorien mit Namen und Farbe frei verwalten
- Update-Suche

**Sonst**

- Material 3, dunkles Design, ab Android 12 auf Wunsch mit den Systemfarben
- Komplett offline, keine Konten, keine Cloud, keine Werbung

## Updates – prüfen in der App, laden im Browser

Unter **Einstellungen → Version und Update** sucht die App auf GitHub nach einer
neueren Fassung. Findet sie eine, zeigt sie an, was neu ist, und öffnet auf
Wunsch den **Browser** mit der APK. Heruntergeladen und installiert wird dort.

Das ist Absicht: Eine App, die Pakete selbst herunterlädt und installiert,
braucht die Berechtigung `REQUEST_INSTALL_PACKAGES` und einen `FileProvider`.
Android warnt dann beim Installieren, und Play Protect stuft so eine App gerne
als riskant ein. Diese App fragt deshalb nur nach der Versionsnummer und
übergibt die Adresse an den Browser – dem das System ohnehin vertraut. Die
einzige Berechtigung dafür ist `INTERNET`.

Die Versionsnummer vergibt GitHub Actions automatisch als `1.0.<Build-Nummer>`,
jeder Push auf `main` erzeugt also ein Release, das die App findet.

## Signatur – wichtig für Updates

Android legt eine neue Version nur dann über eine bestehende Installation, wenn
beide mit **demselben Schlüssel** signiert sind. Ein Debug-Build erzeugt auf
jedem Rechner einen neuen Zufallsschlüssel – damit würde jedes Update an
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` scheitern.

Deshalb baut der Workflow signierte Release-APKs mit einem festen Schlüssel aus
zwei GitHub-Secrets:

| Secret | Inhalt |
|---|---|
| `KEYSTORE_B64` | der Keystore `meine-termine.jks`, base64-kodiert |
| `KEYSTORE_PASSWORD` | das Passwort des Keystores |

Der Keystore selbst liegt **nicht** im Repository (siehe `.gitignore`). Er
gehört gesichert aufbewahrt: Geht er verloren, lässt sich keine Aktualisierung
mehr über eine bestehende Installation legen – dann hilft nur Deinstallieren
und Neuinstallieren, und die eingetragenen Termine sind weg.

## APK bekommen

Bei jedem Push auf `main` baut GitHub Actions die APK automatisch:

1. Reiter **Actions** → letzter Lauf → unter **Artifacts** liegt `meine-termine-apk`
2. oder bequemer auf dem Handy: Reiter **Releases** → neueste Version →
   `meine-termine-<version>.apk` herunterladen

Danach übernimmt die eingebaute Update-Funktion.

Zum Installieren muss in Android einmalig "Installation aus unbekannten Quellen"
für den Browser bzw. den Dateimanager erlaubt werden.

## Nach der Installation einmalig einstellen

Damit die Erinnerungen zuverlässig kommen:

1. Benachrichtigungen erlauben (fragt die App beim ersten Start)
2. **Wecker und Erinnerungen** erlauben (Android 12+)
3. Akku-Optimierung für die App ausschalten

Die App zeigt dafür oben eine Hinweiskarte mit einem Knopf, der direkt in die
richtige Systemeinstellung springt.

## Selbst bauen

```bash
./gradlew assembleDebug
```

Die APK liegt dann unter `app/build/outputs/apk/debug/app-debug.apk`.

## Technik

- Kotlin, Jetpack Compose, Material 3 (mit dynamischen Farben ab Android 12)
- Room als lokale Datenbank (`termine.db`)
- `AlarmManager.setAlarmClock()` + `BootReceiver` für die Erinnerungen
- Update-Prüfung über die GitHub-Releases-API, Download im Browser
  (keine Installationsberechtigung nötig, keine zusätzlichen Bibliotheken)
- minSdk 26 (Android 8.0), targetSdk 35
