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

- Termine mit Bezeichnung, Datum, Uhrzeit, Ort und Notizen
- Frei verwaltbare Kategorien mit Farbe (Hausarzt, Zahnarzt, Diabetologe,
  Augenarzt, Orthopäde, Physiotherapie, Labor, Sonstiges – weitere selbst anlegbar)
- Nächster Termin prominent oben mit Countdown ("noch 2 Tage 3 Std")
- Filter nach Kategorie, vergangene Termine einklappbar
- Beliebig viele Erinnerungen pro Termin
  - Vorgaben: pünktlich, 15 / 30 / 45 Minuten, 1 / 2 / 3 Stunden, 1 / 2 Tage, 1 Woche
  - frei eingebbare Vorlaufzeit in Minuten, Stunden oder Tagen
- Komplett offline, keine Konten, keine Cloud, keine Werbung
- Hinweiskarte in der App, falls Android exakte Alarme oder die
  Akku-Optimierung noch blockiert – mit direktem Sprung in die Einstellungen

## Updates – die App aktualisiert sich selbst

Oben rechts in der App sitzt das Update-Symbol. Ein Tipp darauf fragt das
neueste GitHub-Release ab, vergleicht es mit der installierten Version und
bietet bei Bedarf den Download an. Die APK wird über den System-Download-Manager
geladen (mit Fortschrittsanzeige) und danach direkt dem Paket-Installer
übergeben. Kein Store, kein Konto, kein manuelles Suchen.

Einmalig muss Android der App erlauben, Pakete zu installieren – die App
erkennt das und bietet den Sprung in die passende Einstellung an.

Die Versionsnummer vergibt GitHub Actions automatisch als `1.0.<Build-Nummer>`,
jeder Push auf `main` erzeugt also ein neues Release, das die App findet. Unten
in der Terminliste steht immer, welche Version gerade installiert ist.

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
- Selbst-Update über die GitHub-Releases-API, `DownloadManager` und `FileProvider`
  (ohne zusätzliche Bibliotheken)
- minSdk 26 (Android 8.0), targetSdk 35
