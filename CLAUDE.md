# Projektgedächtnis – Meine Termine

Diese Datei ist für Claude gedacht und wird beim Öffnen des Projekts
automatisch gelesen. Sie beantwortet die Frage: *Worum geht es hier, wie
wird gebaut, was ist absichtlich so, und wo standen wir zuletzt?*

**Bitte beim Weiterarbeiten pflegen** – vor allem den Abschnitt
„Stand" und „Offene Punkte".

---

## Worum es geht

Eine eigenständige Android-App für Arzttermine. Entstanden, weil die
Benachrichtigungen normaler Kalender-Apps unzuverlässig waren. Kein
Google-Sync nötig, Termine werden von Hand eingetragen.

- Repository: https://github.com/ProTech1982/meine-termine (öffentlich)
- Paket: `de.protech.termine`
- Lizenz: MIT, Rechteinhaber ProTech1982

## Technik

| | |
|---|---|
| Sprache | Kotlin |
| Oberfläche | Jetpack Compose, Material 3 |
| Datenbank | Room (über KSP) |
| minSdk / targetSdk | 26 / 35 |

Aufbau unter `app/src/main/java/de/protech/termine/`:

- `MainActivity.kt` – NavHost, alle Ziele; `enableEdgeToEdge()`
- `ui/HomeScreen.kt` – Übersicht: Karte „Als Nächstes", Zeitgruppen,
  Kategoriefilter, gewesene Termine
- `ui/EditScreen.kt` – Termin anlegen und ändern
- `ui/SettingsScreen.kt` – Berechtigungs-Prüfung, Kategorien, Update-Suche
- `ui/AppearanceScreen.kt` – Helligkeit, Akzentfarbe, Kachelform, Deckkraft,
  Hintergrundmuster
- `ui/LegalScreen.kt` – Über & Rechtliches
- `ui/TermineViewModel.kt`, `data/` – Room, DAO, Repository
- `notify/` – `AlarmScheduler`, `ReminderReceiver`, `BootReceiver`
- `look/` – `Appearance` (gespeicherte Einstellungen), `Surfaces` (die
  gemeinsame Kachel `AppCard`), `Patterns` (gerechnete Hintergrundmuster)
- `update/Updater.kt` – Versionsabfrage gegen die GitHub-API

## Bauen und veröffentlichen

**Es wird nicht lokal gebaut.** Jeder Push auf `main` löst
`.github/workflows/build-apk.yml` aus; der Workflow baut `assembleRelease`
und veröffentlicht ein Release.

- Versionsnummer: `1.0.<Lauf-Nummer>`, `versionCode` = Lauf-Nummer.
  Sie wird **nicht** im Repo gepflegt, sondern beim Bauen übergeben
  (`-PappVersionName` / `-PappVersionCode`).
- Release-Tag: `v1.0.x`, Datei `meine-termine-1.0.x.apk`
- Änderungstext im Release = Betreffzeile des letzten Commits
  (bewusst nur `git log -1 --pretty=%s`, damit keine Commit-Anhänge wie
  `Co-Authored-By` im Update-Fenster der App landen)
- Signierung: fester Keystore, Alias `meinetermine`, aus den Secrets
  `KEYSTORE_B64` und `KEYSTORE_PASSWORD`. **Liegt nicht im Repo.**
  Zertifikat SHA-256 beginnt mit `de77abc3`.

Nach dem Push: auf den Workflow warten, dann das Release prüfen.
Ohne gültigen Keystore wird nur debug-signiert – solche APKs lassen sich
nicht über eine signierte Version legen.

## Vor dem Pushen pruefen

Ein gruener Build heisst hier **nicht**, dass die App laeuft - gebaut, aber
nie gestartet. Deshalb laeuft in der Action vor dem Bauen:

- `python3 tools/pruefe-datenbank.py` - prueft, dass zu jeder Room-Version
  eine Migration vorliegt, dass `addMigrations(...)` aufgerufen wird und dass
  **kein** `fallbackToDestructiveMigration` benutzt wird. Letzteres wuerde
  beim Update die Termine des Nutzers loeschen. Room meldet eine fehlende
  Migration erst beim Start auf einem Geraet, auf dem schon Daten liegen -
  auf einer Neuinstallation faellt es nie auf.
- Grundsaetzlich: Alles, was erst zur Laufzeit geprueft wird, faellt im Build
  nicht auf. Im Zweifel einen Pruefschritt in `tools/` dazulegen, statt sich
  auf den gruenen Haken zu verlassen.

## Absichtliche Entscheidungen (nicht versehentlich so)

- **`AlarmManager.setAlarmClock()`** statt normaler Alarme. Nur diese sind
  von Doze und Akku-Optimierung ausgenommen. Nebenwirkung: das Wecker-Symbol
  in der Statusleiste. Das ist gewollt, nicht zu „beheben".
- Alarme werden nach Neustart, App-Update und Zeitumstellung neu gesetzt.
- **Kein `REQUEST_INSTALL_PACKAGES`, kein FileProvider, kein eigener
  Download.** Die App fragt nur nach, ob es etwas Neues gibt, und übergibt
  die Adresse an den Browser. Grund: Internetzugriff plus Selbst-Installation
  ist dasselbe Muster wie bei Schadsoftware, Play Protect warnt dann.
  Diese Entscheidung bitte nicht rückgängig machen.
- Die App ist offen als KI-generiert gekennzeichnet – in README und unter
  „Über & Rechtliches".

## Arbeitsweise mit ProTech1982

- Alles auf Deutsch: Oberfläche, Commit-Nachrichten, Code-Kommentare.
- Er arbeitet überwiegend vom Handy aus und baut nichts selbst.
  Claude übernimmt Git, Push und das Beobachten des Builds und meldet erst
  zurück, wenn das Release fertig ist.
- Die Play-Protect-Warnung beim Installieren („kennt von diesem Entwickler
  noch keine anderen Apps") ist normal für selbst signierte APKs und lässt
  sich nicht aus der App heraus abstellen. Weg wäre sie nur über ein
  Play-Store-Entwicklerkonto (einmalig 25 $, Ausweisprüfung, und für neue
  Privatkonten ein geschlossener Test mit 12 Testern über 14 Tage).

## Stand

**3. November 2026, Version 1.0.17**

Zuletzt gemacht:

- 1.0.17: **Aerzte als Schnellauswahl.** Eigene Tabelle `doctors` (Name,
  optionale `categoryId`, optionaler Ort), Datenbank auf Version 3, Migration
  `FROM_2_TO_3`. Verwaltung in `DoctorScreen.kt`, Chips im `EditScreen` unter
  dem Feld "Bezeichnung": ein Tipp setzt Name, Kategorie und Ort, nochmal
  tippen leert das Namensfeld. Das Feld bleibt ein normales Textfeld, der
  Nutzer kann hinter dem Namen weiterschreiben ("Dr. Weber - Kontrolle");
  der Chip ist dann nicht mehr hervorgehoben, weil der Text nicht mehr
  exakt passt. Die Liste startet leer - keine Vorgaben, das sind seine
  eigenen Aerzte.
- 1.0.15: Kennzeichen lesbar gemacht. Die reine Kennzeichenfarbe als
  Schriftfarbe war auf dunklem Grund kaum zu entziffern; jetzt rechnet
  `readableOn()` in `HomeScreen.kt` die Schrift gegen den Untergrund
  (Kontrast mindestens 4.5), dazu eine zarte Fuellung und eine Stufe
  groessere Schrift. **Merke:** Eine Nutzerfarbe nie ungeprueft als
  Schriftfarbe verwenden.

- 1.0.14: **Kennzeichen** - ein Zusatzschild neben der Kategorie, zum
  Beispiel "Labor" neben "Diabetologe". Ein Termin kann mehrere tragen;
  angezeigt werden sie alphabetisch (`AppointmentFull.sortedMarkers`), als
  umrandete Pille in eigener Farbe (`MarkerPill` in `HomeScreen.kt`), damit
  die gefuellte Kategorie die Hauptsache bleibt.
  Datenbank auf Version 2: neue Tabellen `markers` und `appointment_markers`
  (Verknuepfung ueber `@Junction`), Migration in `data/Migrations.kt`.
  Auswahl im `EditScreen`, Verwaltung in `MarkerScreen.kt` (wie
  `CategoryScreen`), zweite Filterreihe in der Uebersicht, die sich mit dem
  Kategoriefilter kombinieren laesst. Vorgaben: Arzttermin, Blutprobe,
  Kontrolle, Labor.

- 1.0.6: Seite „Darstellung" mit Helligkeit, 10 Akzentfarben, Kachelform,
  Deckkraft und 10 gerechneten Hintergrundmustern; gemeinsame Kachel `AppCard`
- 1.0.10: Speichern-Knopf im Termin-Fenster bekommt
  `navigationBarsPadding()`, saß vorher unter der Android-Navigationsleiste
- 1.0.11: Termine öffnen sich **nur noch über das Stift-Symbol** in der
  Kachel. Die Kachel selbst ist nicht mehr anklickbar – vorher kam man
  versehentlich ins Bearbeiten (`AppCard` ohne `onClick`, `IconButton` mit
  `Icons.Default.Edit` in `HomeScreen.kt`, sowohl in `NextCard` als auch in
  `AppointmentRow`)
- 1.0.12: Release-Text ohne Commit-Anhänge

## Offene Punkte

- Klarname statt „ProTech1982" in Lizenz und Rechtlichem? Noch nicht entschieden.
- Soll Claude im KI-Hinweis namentlich genannt bleiben? Noch offen.
- Wahl der Repo-Lizenz war bisher MIT, nicht abschließend bestätigt.
