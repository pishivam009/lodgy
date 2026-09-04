# CLAUDE.md

Lodgy — offline-first native Android app (Kotlin + Jetpack Compose) for hostel/PG
management by a single warden. No backend, no network permissions; all data
lives on-device (Room/SQLite), with manual export/import for phone migration.

Design doc: `docs/DESIGN.md` — read this before making architecture or schema
decisions.

User manual: `docs/USER-MANUAL.md` — what the app does, from the warden's side.

Status: built. All epics on the board (LODGY-1..62, minus the one Won't Do) are
implemented; the board tracks what is still awaiting verification.
