# Gazel Arena (NeoForge 1.21.1, Tensura-kompatibel)

Fügt den Befehl `/gazel` hinzu. Er teleportiert dich in einen Thronsaal (Boss-Arena), in dem
**Gazel Dwargo** aus *Tensura: Reincarnated* gegen dich kämpft.

- `/gazel` – Arena betreten, Kampf startet
- `/gazel leave` – Arena verlassen (Arena wird zurückgesetzt)

## Ablauf
- Die Arena wird automatisch gebaut (Overworld, Standard: X/Z = 1000000, Y = 200).
- **Niederlage:** Du stirbst nicht wirklich – du wirst voll geheilt zurückteleportiert (kein Item-Verlust),
  der Boss verschwindet, die Arena wird komplett neu aufgebaut.
- **Sieg:** Du hast 20 Sekunden (einstellbar), um die Beute einzusammeln, dann geht es zurück und die Arena wird zurückgesetzt.
- Es kann immer nur ein Spieler gleichzeitig kämpfen.
- Loggt sich der Spieler aus oder verlässt die Arena, wird alles zurückgesetzt; beim nächsten Login wird er an seinen alten Ort gebracht.

## Config
`config/gazelarena-common.toml` (entsteht beim ersten Start):

| Option | Bedeutung |
|---|---|
| `bossEntity` | Entity-ID, Standard `tensura:gazel_dwargo`. Wird sie nicht gefunden, sucht die Mod selbst nach einer Entity mit „gazel“ im Namen. |
| `arenaX`, `arenaY`, `arenaZ` | Position der Arena im Overworld |
| `winReturnSeconds` | Sekunden bis zum Rücktransport nach dem Sieg |
| `victoryCommands` | Befehle bei Sieg, `%player%` = Spielername, z. B. `["give %player% minecraft:diamond 5"]` |

Tipp: Die genaue Boss-ID kannst du im Spiel mit `/summon tensura:` + Tab-Taste nachsehen.

## Als JAR über GitHub bauen
1. Auf github.com ein neues Repository anlegen (z. B. `gazelarena`).
2. Den **Inhalt** dieses Ordners hochladen (inkl. des versteckten Ordners `.github`), oder per Git:
   ```bash
   git init
   git add .
   git commit -m "Gazel Arena"
   git branch -M main
   git remote add origin https://github.com/DEIN-NAME/gazelarena.git
   git push -u origin main
   ```
3. Im Repository auf den Reiter **Actions** gehen. Der Workflow „Build mod“ läuft automatisch (ca. 2–5 Minuten).
4. Nach dem Durchlauf den Lauf öffnen → unten unter **Artifacts** → `gazelarena-jar` herunterladen, ZIP entpacken → `gazelarena-1.0.0.jar`.
5. Die JAR in den `mods`-Ordner legen (Server **und** Client-Modpack ist nicht nötig – die Mod reicht auf dem Server/Singleplayer-Host).

Optional: Einen Tag wie `v1.0.0` pushen, dann hängt der Workflow die JAR automatisch an ein GitHub-Release.

## Lokal bauen
Java 21 und Gradle (8.10+) installieren, dann `gradle build` – die JAR liegt in `build/libs/`.
