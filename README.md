# Lodgy

Lodgy is an offline-first Android app for a single warden running one or
more hostels/PGs: property setup, tenant onboarding, rent collection,
expenses, and reporting — all on-device, no backend, no account.

Full product spec: [`docs/DESIGN.md`](docs/DESIGN.md).

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM |
| Local DB | Room (SQLite) — UUID primary keys, no auto-increment |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| DI | Hilt |
| Background work | WorkManager |
| Backup/restore | Storage Access Framework, zip of DB + photos |

No network permissions are requested. Data portability is manual
export/import, not cloud sync.

## Building

```
./gradlew assembleDebug   # build a debug APK
./gradlew test            # unit tests
```

Requires Android Studio / an Android SDK with `compileSdk 37` installed.
minSdk is 24.

## Project structure

```
app/src/main/java/com/lodgy/app/
  data/           Room entities, DAOs, database, Hilt module
  ui/nav/         Top-level NavHost, bottom navigation, destinations
  ui/screens/     Feature screens
  ui/theme/       Material 3 theme, color/type tokens
docs/DESIGN.md    Product & architecture spec — read before schema/architecture changes
board/            Ticket-based build tracker (see below)
```

## The board

Work is tracked as tickets under `board/tickets/LODGY-N.json`, rendered to
a static Kanban view at `board/board.html`. Each ticket carries a full
history of who did what and why — regenerate the HTML with:

```
python scripts/generate_board.py
```

Tickets move `Todo → In Progress → Testing → Done → Delivered`, driven by
four roles (each a Claude Code skill under `.claude/skills/`):

- **Product Owner** — scopes and prioritizes tickets, does final acceptance.
- **Developer** — implements a ticket's acceptance criteria against
  `docs/DESIGN.md`, moves `Todo → In Progress → Testing`.
- **Tester** — verifies a ticket in `Testing` against its acceptance
  criteria (and the design doc), moves it to `Done` or bounces it back
  to `In Progress` with a specific failure note.
- **User** — does real UAT on `Done` tickets from a warden's actual
  day-to-day perspective, moves them to `Delivered` or files new tickets
  for gaps noticed along the way.

## Design

Low-fidelity wireframes for every screen in the app, organized by epic,
were built before any UI code:

- [Wireframe set](https://claude.ai/code/artifact/90f2f9b6-b4ab-49f7-b4a3-db3730230a8b) — every screen, organized by epic
- [Warden's Guide](https://claude.ai/code/artifact/a7741ef9-03a3-4451-bd53-6bec2d2404f0) — the same screens walked through task by task, for the actual end user
