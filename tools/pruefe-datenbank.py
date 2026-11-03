#!/usr/bin/env python3
"""
Prueft, ob zu jeder Datenbank-Version eine Migration vorliegt.

Hintergrund: Room wirft beim Start eine Ausnahme, wenn die Versionsnummer
hochgesetzt wurde, aber der Weg von der alten zur neuen Version fehlt. Das
faellt beim Bauen nicht auf - die App stuerzt erst auf einem Geraet ab, auf
dem schon Termine liegen. Auf einem frisch installierten Geraet bleibt es
unbemerkt.

Geprueft wird: Fuer jede Stufe von 1 bis zur aktuellen Version muss es eine
Migration(n, n+1) geben, und sie muss in der Datenbank eingetragen sein.

Laeuft in der GitHub-Action vor dem Bauen.
"""

import re
import sys
from pathlib import Path

WURZEL = Path(__file__).resolve().parent.parent
DB = WURZEL / "app/src/main/java/de/protech/termine/data/TermineDatabase.kt"
MIGRATIONEN = WURZEL / "app/src/main/java/de/protech/termine/data/Migrations.kt"


def main():
    if not DB.is_file():
        print(f"Datei nicht gefunden: {DB}")
        return 1

    db_text = DB.read_text(encoding="utf-8")

    treffer = re.search(r"version\s*=\s*(\d+)", db_text)
    if not treffer:
        print("Keine Versionsnummer in TermineDatabase.kt gefunden.")
        return 1
    version = int(treffer.group(1))

    if version == 1:
        print("Datenbank steht auf Version 1, keine Migration noetig.")
        return 0

    if not MIGRATIONEN.is_file():
        print(f"Version {version}, aber keine Migrations.kt vorhanden.")
        return 1

    mig_text = MIGRATIONEN.read_text(encoding="utf-8")
    vorhanden = {
        (int(a), int(b)) for a, b in re.findall(r"Migration\(\s*(\d+)\s*,\s*(\d+)\s*\)", mig_text)
    }

    fehler = []
    for stufe in range(1, version):
        if (stufe, stufe + 1) not in vorhanden:
            fehler.append(f"Migration von {stufe} nach {stufe + 1} fehlt in Migrations.kt")

    if "addMigrations" not in db_text:
        fehler.append("TermineDatabase.kt ruft addMigrations(...) nicht auf")

    if "fallbackToDestructiveMigration" in db_text:
        fehler.append(
            "TermineDatabase.kt benutzt fallbackToDestructiveMigration - "
            "das loescht beim Update die Termine des Nutzers"
        )

    if fehler:
        print("Datenbank-Pruefung fehlgeschlagen:")
        for eintrag in fehler:
            print(f"  - {eintrag}")
        return 1

    print(f"Datenbank-Pruefung in Ordnung (Version {version}, alle Migrationen vorhanden).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
